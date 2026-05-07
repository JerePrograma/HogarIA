package com.hogaria.service;

import com.hogaria.dto.DashboardDtos.*;import com.hogaria.entity.*;import com.hogaria.exception.ForbiddenException;import com.hogaria.repository.*;import java.math.*;import java.time.*;import java.util.*;import java.util.stream.*;import org.springframework.stereotype.Service;
@Service
public class DashboardService {
  private final FinancialProfileRepository profileRepository; private final MoneyTransactionRepository transactionRepository; private final CategoryRepository categoryRepository;
  public DashboardService(FinancialProfileRepository profileRepository, MoneyTransactionRepository transactionRepository, CategoryRepository categoryRepository){this.profileRepository=profileRepository;this.transactionRepository=transactionRepository;this.categoryRepository=categoryRepository;}
  public DashboardSummaryResponse getMonthlySummary(UUID userId, UUID profileId, int year, int month){profileRepository.findByIdAndUserId(profileId,userId).orElseThrow(()->new ForbiddenException("Profile does not belong to user")); var from=LocalDate.of(year,month,1); var to=from.withDayOfMonth(from.lengthOfMonth()); var txs=transactionRepository.findByProfileIdAndBudgetDateBetween(profileId,from,to).stream().filter(t->t.getStatus()==MoneyTransaction.Status.CONFIRMED).toList(); var categories=categoryRepository.findAllById(txs.stream().map(MoneyTransaction::getCategoryId).collect(Collectors.toSet())).stream().collect(Collectors.toMap(Category::getId,c->c));
    BigDecimal income=sum(txs.stream().filter(t->t.getMovementType()==MoneyTransaction.MovementType.INCOME).map(MoneyTransaction::getAmount).toList());
    BigDecimal fixed=sumByType(txs,categories,Category.Type.FIXED_EXPENSE); BigDecimal variable=sumByType(txs,categories,Category.Type.VARIABLE_EXPENSE);
    BigDecimal savings=sum(txs.stream().filter(t->t.getMovementType()==MoneyTransaction.MovementType.SAVING || categories.get(t.getCategoryId())!=null && categories.get(t.getCategoryId()).getType()==Category.Type.SAVING).map(MoneyTransaction::getAmount).toList());
    BigDecimal totalExpenses=fixed.add(variable); BigDecimal balance=income.subtract(totalExpenses).subtract(savings);
    BigDecimal fp=percent(fixed,income), vp=percent(variable,income), sp=percent(savings,income);
    String health=balance.signum()<0?"CRITICAL":(sp.compareTo(new BigDecimal("20"))>=0&&fp.compareTo(new BigDecimal("50"))<=0&&vp.compareTo(new BigDecimal("30"))<=0?"EXCELLENT":(savings.signum()>0?"HEALTHY":"WARNING"));
    var breakdown=txs.stream().collect(Collectors.groupingBy(MoneyTransaction::getCategoryId)).entrySet().stream().map(e->{var c=categories.get(e.getKey()); var total=sum(e.getValue().stream().map(MoneyTransaction::getAmount).toList()); return new CategorySummaryResponse(e.getKey(),c==null?"Unknown":c.getName(),c==null?null:c.getType(),total,percent(total,income),e.getValue().size());}).toList();
    return new DashboardSummaryResponse(new MonthlyBalanceResponse(income,totalExpenses,savings,balance),new FiftyThirtyTwentyResponse(fp,vp,sp),fixed,variable,health,breakdown);
  }
  private BigDecimal sumByType(List<MoneyTransaction> txs, Map<UUID,Category> m, Category.Type type){ return sum(txs.stream().filter(t->m.get(t.getCategoryId())!=null && m.get(t.getCategoryId()).getType()==type).map(MoneyTransaction::getAmount).toList()); }
  private BigDecimal sum(List<BigDecimal> values){return values.stream().reduce(BigDecimal.ZERO,BigDecimal::add);} private BigDecimal percent(BigDecimal a, BigDecimal income){ if(income.signum()==0) return BigDecimal.ZERO; return a.multiply(new BigDecimal("100")).divide(income,2,RoundingMode.HALF_UP);} }
