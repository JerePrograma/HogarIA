package com.hogaria.service;

import com.hogaria.dto.PlanningSuggestionDtos.*;
import com.hogaria.entity.*;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.repository.*;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class MonthlyPlanSuggestionService {
  private final MoneyTransactionRepository txRepo; private final MonthlyPlanItemRepository itemRepo; private final AccountRepository accountRepo; private final CategoryRepository categoryRepo; private final FinancialProfileRepository profileRepo;
  public MonthlyPlanSuggestionService(MoneyTransactionRepository txRepo, MonthlyPlanItemRepository itemRepo, AccountRepository accountRepo, CategoryRepository categoryRepo, FinancialProfileRepository profileRepo){this.txRepo=txRepo;this.itemRepo=itemRepo;this.accountRepo=accountRepo;this.categoryRepo=categoryRepo;this.profileRepo=profileRepo;}

  public PlanningSuggestionResponse suggest(UUID userId, UUID profileId, PlanningSuggestionRequest req){
    profileRepo.findByIdAndUserId(profileId,userId).orElseThrow(()->new ForbiddenException("El perfil no pertenece al usuario actual."));
    var matches=new ArrayList<Match>();
    txRepo.findByProfileId(profileId).forEach(tx->matches.add(scoreTx(req,tx)));
    itemRepo.findByProfileId(profileId).stream()
        .filter(i->i.getStatus()!=MonthlyPlanItem.Status.CANCELLED)
        .forEach(i->matches.add(scoreItem(req,i)));
    matches.removeIf(m->m.score<25 || (m.accountId==null && m.categoryId==null));
    if(matches.isEmpty()) return new PlanningSuggestionResponse(null,null,SuggestionConfidence.NONE,List.of("Sin sugerencias confiables por ahora."));
    matches.sort(Comparator.comparingInt((Match m)->m.score).reversed().thenComparing((Match m)->m.recency, Comparator.reverseOrder()));
    var top=matches.get(0);
    var reasons=new ArrayList<>(top.reasons);
    SuggestedAccount acc=null; SuggestedCategory cat=null;
    if(top.accountId!=null){ var a=accountRepo.findByIdAndProfileId(top.accountId,profileId).orElse(null); if(a!=null) acc=new SuggestedAccount(a.getId(),a.getName(),conf(top.score),"Se usó esta cuenta en movimientos similares."); }
    if(top.categoryId!=null){ var c=categoryRepo.findById(top.categoryId).orElse(null); if(c!=null && (c.getProfileId()==null || Objects.equals(c.getProfileId(),profileId))) cat=new SuggestedCategory(c.getId(),c.getName(),conf(top.score),"Se usó esta categoría en movimientos similares."); }
    return new PlanningSuggestionResponse(acc,cat,conf(top.score),reasons);
  }

  private Match scoreTx(PlanningSuggestionRequest req, MoneyTransaction tx){
    int s=0; var reasons=new ArrayList<String>();
    var title=norm(req.title()); var desc=norm(tx.getDescription());
    if(!blank(req.counterparty()) && norm(req.counterparty()).equals(desc)){ s+=50; reasons.add("Coincide la contraparte '"+req.counterparty()+"'."); }
    if(!title.isEmpty() && (!desc.isEmpty() && (title.contains(desc)||desc.contains(title)))){ s+=40; reasons.add("Coincide con operaciones anteriores de '"+tx.getDescription()+"'."); }
    if(shared(title,desc)>=2){ s+=20; reasons.add("Coincide con operaciones anteriores de '"+tx.getDescription()+"'."); }
    if(compatible(req.type(),tx.getMovementType())){ s+=15; reasons.add("Coincide el tipo de movimiento."); }
    var amount = amount(req);
    if(amount!=null && tx.getAmount()!=null && similar(amount,tx.getAmount())){ s+=10; reasons.add("El monto es similar a registros anteriores."); }
    if(tx.getBudgetDate()!=null && tx.getBudgetDate().isAfter(LocalDate.now().minusMonths(3))){ s+=10; }
    if(tx.getStatus()==MoneyTransaction.Status.CONFIRMED){ s+=10; }
    return new Match(s, tx.getAccountId(), tx.getCategoryId(), tx.getUpdatedAt(), reasons);
  }

  private Match scoreItem(PlanningSuggestionRequest req, MonthlyPlanItem i){ int s=0; var reasons=new ArrayList<String>(); var title=norm(req.title()); var hist=norm(i.getTitle()); var cp=norm(i.getCounterparty());
    if(!blank(req.counterparty()) && norm(req.counterparty()).equals(cp)){ s+=50; reasons.add("Coincide la contraparte '"+i.getCounterparty()+"'."); }
    if(!title.isEmpty() && !hist.isEmpty() && (title.contains(hist)||hist.contains(title))){ s+=40; reasons.add("Coincide con operaciones anteriores de '"+i.getTitle()+"'."); }
    if(shared(title,hist)>=2 || shared(title,cp)>=2){ s+=20; reasons.add("Coincide con operaciones anteriores de '"+i.getTitle()+"'."); }
    if(compatible(req.type(),i.getType())){ s+=15; reasons.add("Coincide el tipo de movimiento."); }
    var amount=amount(req); var h=i.getAmount()!=null?i.getAmount():i.getMinAmount();
    if(amount!=null && h!=null && similar(amount,h)){ s+=10; reasons.add("El monto es similar a registros anteriores."); }
    if(i.getExpectedDate()!=null && i.getExpectedDate().isAfter(LocalDate.now().minusMonths(3))){ s+=10; }
    if(i.getTransactionId()!=null || i.getStatus()==MonthlyPlanItem.Status.PAID || i.getStatus()==MonthlyPlanItem.Status.COLLECTED){ s+=10; }
    return new Match(s,i.getAccountId(),i.getCategoryId(),i.getUpdatedAt(),reasons);
  }

  record Match(int score, UUID accountId, UUID categoryId, LocalDateTime recency, List<String> reasons){}
  private static final Pattern NON_ALNUM=Pattern.compile("[^\\p{Alnum}\\s]");
  private String norm(String t){ if(t==null) return ""; var x=Normalizer.normalize(t.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}+",""); x=NON_ALNUM.matcher(x).replaceAll(" ").replaceAll("\\s+"," ").trim(); return x; }
  private int shared(String a,String b){ var sa=tokens(a); var sb=tokens(b); sa.retainAll(sb); return sa.size(); }
  private Set<String> tokens(String t){ var out=new HashSet<String>(); for(var p:t.split(" ")) if(p.length()>2 && !p.chars().allMatch(Character::isDigit)) out.add(p); return out; }
  private boolean compatible(MonthlyPlanItem.Type t, MoneyTransaction.MovementType mt){ if(t==null||mt==null) return false; return switch(t){ case INCOME,RECOVERY -> mt==MoneyTransaction.MovementType.INCOME; case EXPENSE,DEBT -> mt==MoneyTransaction.MovementType.EXPENSE; case SAVING -> mt==MoneyTransaction.MovementType.SAVING; case TRANSFER -> mt==MoneyTransaction.MovementType.TRANSFER; default -> false;}; }
  private boolean compatible(MonthlyPlanItem.Type t, MonthlyPlanItem.Type h){ if(t==null||h==null) return false; return switch(t){ case INCOME -> h==MonthlyPlanItem.Type.INCOME||h==MonthlyPlanItem.Type.RECOVERY; case RECOVERY -> h==MonthlyPlanItem.Type.INCOME||h==MonthlyPlanItem.Type.RECOVERY; case EXPENSE,DEBT -> h==MonthlyPlanItem.Type.EXPENSE||h==MonthlyPlanItem.Type.DEBT; default -> t==h;}; }
  private BigDecimal amount(PlanningSuggestionRequest req){ if(req.amount()!=null) return req.amount(); if(req.minAmount()!=null&&req.maxAmount()!=null) return req.minAmount().add(req.maxAmount()).divide(new BigDecimal("2")); if(req.minAmount()!=null) return req.minAmount(); return req.maxAmount(); }
  private boolean similar(BigDecimal a, BigDecimal b){ if(b.signum()==0) return a.compareTo(BigDecimal.ZERO)==0; return a.subtract(b).abs().divide(b.abs(),4, java.math.RoundingMode.HALF_UP).compareTo(new BigDecimal("0.2"))<0; }
  private boolean blank(String s){ return s==null||s.trim().isEmpty(); }
  private SuggestionConfidence conf(int s){ if(s>=80) return SuggestionConfidence.HIGH; if(s>=50) return SuggestionConfidence.MEDIUM; if(s>=25) return SuggestionConfidence.LOW; return SuggestionConfidence.NONE; }
}
