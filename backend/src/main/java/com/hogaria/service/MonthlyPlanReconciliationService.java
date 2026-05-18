package com.hogaria.service;

import com.hogaria.dto.MonthlyPlanReconciliationDtos.*;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.entity.MonthlyPlanItem;
import com.hogaria.entity.MonthlyPlanTransactionMatch;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.exception.NotFoundException;
import com.hogaria.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MonthlyPlanReconciliationService {
  private final FinancialProfileRepository profiles; private final MonthlyPlanItemRepository itemRepo; private final MoneyTransactionRepository txRepo; private final MonthlyPlanTransactionMatchRepository matchRepo;
  public MonthlyPlanReconciliationService(FinancialProfileRepository profiles, MonthlyPlanItemRepository itemRepo, MoneyTransactionRepository txRepo, MonthlyPlanTransactionMatchRepository matchRepo){this.profiles=profiles;this.itemRepo=itemRepo;this.txRepo=txRepo;this.matchRepo=matchRepo;}
  private void ensureProfile(UUID profileId, UUID userId){profiles.findByIdAndUserId(profileId,userId).orElseThrow(()->new ForbiddenException("Profile does not belong to user"));}

  public MonthlyPlanReconciliationSummary getSummary(UUID userId, UUID profileId, int year, int month){ ensureProfile(profileId,userId); var items=itemRepo.findByProfileIdAndPeriodYearAndPeriodMonth(profileId,year,month); var from=LocalDate.of(year,month,1); var txs=txRepo.findByProfileIdAndBudgetDateBetween(profileId,from,from.withDayOfMonth(from.lengthOfMonth()));
    var matches = findMatches(profileId, items, txs);
    var matchedTxIds = matches.stream().map(MonthlyPlanTransactionMatch::getMoneyTransactionId).collect(Collectors.toSet());
    var unplanned = txs.stream().filter(tx->tx.getStatus()== MoneyTransaction.Status.CONFIRMED).filter(tx-> !matchedTxIds.contains(tx.getId())).toList();
    var dedupUnplanned = unplanned.stream().collect(Collectors.toMap(MoneyTransaction::getId, Function.identity(), (a,b)->a)).values();
    var unplannedDtos=dedupUnplanned.stream().map(tx->new UnplannedTransaction(tx.getId(),tx.getBudgetDate(),tx.getDescription(),tx.getAccountId(),tx.getCategoryId(),tx.getMovementType().name(),tx.getAmount(),tx.getStatus().name())).toList();
    var unplannedTotal=dedupUnplanned.stream().map(MoneyTransaction::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
    var suggestions = buildSuggestions(items, txs, matchedTxIds);
    var itemMap = items.stream().collect(Collectors.toMap(MonthlyPlanItem::getId, Function.identity()));
    var byItem = matches.stream().collect(Collectors.groupingBy(MonthlyPlanTransactionMatch::getMonthlyPlanItemId));
    var planItems = items.stream().map(item->{
      var m=byItem.getOrDefault(item.getId(), List.of()); var matched = m.stream().map(MonthlyPlanTransactionMatch::getMatchedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
      var planned = item.getAmount()==null?BigDecimal.ZERO:item.getAmount(); var remaining = planned.subtract(matched);
      var exec=remaining.signum()<=0?"MATCHED":matched.signum()>0?"PARTIAL":"PENDING";
      return new PlanItemReconciliation(item.getId(), item.getTitle(), item.getType(), planned, matched, remaining, exec, m.stream().map(mm->new TransactionMatch(mm.getId(),mm.getMonthlyPlanItemId(),mm.getMoneyTransactionId(),mm.getMatchedAmount(),mm.getMatchType(),mm.getConfidence())).toList());
    }).toList();
    var plannedTotal=planItems.stream().map(PlanItemReconciliation::plannedAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
    var matchedTotal=planItems.stream().map(PlanItemReconciliation::matchedAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
    return new MonthlyPlanReconciliationSummary(plannedTotal, matchedTotal, plannedTotal.subtract(matchedTotal), unplannedTotal, unplannedDtos.size(), suggestions.size(), unplannedDtos, suggestions, planItems);
  }

  public List<MonthlyPlanTransactionMatch> findMatches(UUID profileId, List<MonthlyPlanItem> items, List<MoneyTransaction> txs){
    var itemIds=items.stream().map(MonthlyPlanItem::getId).toList(); var txIds=txs.stream().map(MoneyTransaction::getId).toList();
    var all=new ArrayList<MonthlyPlanTransactionMatch>(); all.addAll(matchRepo.findByProfileIdAndMonthlyPlanItemIdIn(profileId,itemIds)); all.addAll(matchRepo.findByProfileIdAndMoneyTransactionIdIn(profileId,txIds));
    for(var i:items){ if(i.getTransactionId()!=null){ boolean exists=all.stream().anyMatch(m->Objects.equals(m.getMonthlyPlanItemId(),i.getId())&&Objects.equals(m.getMoneyTransactionId(),i.getTransactionId())); if(!exists) all.add(MonthlyPlanTransactionMatch.builder().id(UUID.randomUUID()).profileId(profileId).monthlyPlanItemId(i.getId()).moneyTransactionId(i.getTransactionId()).matchedAmount(i.getAmount()==null?BigDecimal.ZERO:i.getAmount()).matchType(MonthlyPlanTransactionMatch.MatchType.SYSTEM_CONVERSION).confidence(MonthlyPlanTransactionMatch.Confidence.HIGH).build()); }}
    return all.stream().collect(Collectors.toMap(m->m.getMonthlyPlanItemId()+":"+m.getMoneyTransactionId(), Function.identity(), (a,b)->a)).values().stream().toList();
  }

  private List<SuggestedPlanTransactionMatch> buildSuggestions(List<MonthlyPlanItem> items, List<MoneyTransaction> txs, Set<UUID> matchedTxIds){
    var unmatched = txs.stream().filter(tx->tx.getStatus()==MoneyTransaction.Status.CONFIRMED).filter(tx->!matchedTxIds.contains(tx.getId())).toList();
    var out = new ArrayList<SuggestedPlanTransactionMatch>();
    for(var item: items){ if(item.getAmount()==null) continue; for(var tx: unmatched){ if(compatible(item,tx)&&item.getAmount().compareTo(tx.getAmount())==0){ out.add(new SuggestedPlanTransactionMatch(item.getId(), tx.getId(), tx.getAmount(), "HIGH", List.of("Tipo y monto coinciden"))); } } }
    return out;
  }
  private boolean compatible(MonthlyPlanItem item, MoneyTransaction tx){ return switch (item.getType()){ case INCOME,RECOVERY -> tx.getMovementType()== MoneyTransaction.MovementType.INCOME; case EXPENSE,DEBT -> tx.getMovementType()== MoneyTransaction.MovementType.EXPENSE; case SAVING -> tx.getMovementType()== MoneyTransaction.MovementType.SAVING; default -> false;}; }

  public TransactionMatch confirm(UUID userId, UUID profileId, ConfirmPlanTransactionMatchPayload p){ ensureProfile(profileId,userId); var existing=matchRepo.findByProfileIdAndMonthlyPlanItemIdAndMoneyTransactionId(profileId,p.monthlyPlanItemId(),p.moneyTransactionId()).orElse(null);
    var m=existing==null?MonthlyPlanTransactionMatch.builder().profileId(profileId).monthlyPlanItemId(p.monthlyPlanItemId()).moneyTransactionId(p.moneyTransactionId()).build():existing;
    m.setMatchedAmount(p.matchedAmount()); m.setMatchType(p.matchType()==null?MonthlyPlanTransactionMatch.MatchType.MANUAL:p.matchType()); m.setConfidence(p.confidence()==null?MonthlyPlanTransactionMatch.Confidence.HIGH:p.confidence()); m=matchRepo.save(m);
    return new TransactionMatch(m.getId(),m.getMonthlyPlanItemId(),m.getMoneyTransactionId(),m.getMatchedAmount(),m.getMatchType(),m.getConfidence()); }

  public void delete(UUID userId, UUID profileId, UUID matchId){ ensureProfile(profileId,userId); var m=matchRepo.findByIdAndProfileId(matchId,profileId).orElseThrow(()->new NotFoundException("Match no encontrado")); matchRepo.delete(m); }
}
