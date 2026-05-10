package com.hogaria.service;

import com.hogaria.dto.MonthlyPlanDtos.MonthlyPlanItemCreateRequest;
import com.hogaria.dto.QuickCaptureDtos.*;
import com.hogaria.entity.MonthlyPlanItem;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.repository.FinancialProfileRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.regex.*;
import org.springframework.stereotype.Service;

@Service
public class MonthlyPlanQuickCaptureService {
  private final FinancialProfileRepository profiles;
  public MonthlyPlanQuickCaptureService(FinancialProfileRepository profiles){this.profiles=profiles;}

  public QuickCapturePreviewResponse preview(UUID userId, UUID profileId, QuickCapturePreviewRequest request){
    profiles.findByIdAndUserId(profileId,userId).orElseThrow(()->new ForbiddenException("El perfil no pertenece al usuario actual."));
    String raw=request.rawText()==null?"":request.rawText().trim();
    List<String> warnings=new ArrayList<>();
    Integer defaultYear=request.defaultYear()!=null?request.defaultYear():LocalDate.now().getYear();
    Integer defaultMonth=request.defaultMonth()!=null?request.defaultMonth():LocalDate.now().getMonthValue();

    ParsedDate parsedDate=parseDate(raw,defaultYear,defaultMonth,warnings);
    ParsedInstallment parsedInstallment=parseInstallment(raw,warnings);
    ParsedAmount parsedAmount=parseAmount(raw,warnings,parsedInstallment.detectedText());
    ParsedRecovery parsedRecovery=parseRecovery(raw);
    String counterparty=detectCounterparty(raw, parsedRecovery.detectedCounterparty());
    var typeRes=inferType(raw, parsedAmount.hasAmount, parsedRecovery.hasRecoverySignal, warnings);
    MonthlyPlanItem.Type type=typeRes.type;
    if(typeRes.weak) warnings.add("No pude inferir el tipo con alta confianza; revisalo antes de confirmar.");
    MonthlyPlanItem.Priority priority=inferPriority(raw);
    MonthlyPlanItem.Status status=!parsedAmount.hasAmount?MonthlyPlanItem.Status.DRAFT:(parsedDate.expectedDate!=null?MonthlyPlanItem.Status.SCHEDULED:MonthlyPlanItem.Status.ESTIMATED);
    String title=buildTitle(raw);
    if(title.isBlank()) title=raw.length()>160?raw.substring(0,160):raw;

    QuickCaptureConfidence confidence=confidenceOf(title,type,parsedAmount,parsedDate,warnings);
    var parsed=new MonthlyPlanItemCreateRequest(type,title,null,parsedDate.expectedDate,parsedDate.periodYear,parsedDate.periodMonth,
      parsedAmount.amount,parsedAmount.minAmount,parsedAmount.maxAmount,request.defaultCurrency()==null?"ARS":request.defaultCurrency().toUpperCase(),
      parsedRecovery.recoveryAmount,parsedRecovery.recoveryPercent,counterparty,parsedInstallment.number,parsedInstallment.total,priority,status,MonthlyPlanItem.Source.QUICK_CAPTURE,null,null);

    return new QuickCapturePreviewResponse(raw,confidence,warnings,parsed,parsedDate.detectedText,parsedAmount.detectedAmountText,parsedAmount.detectedRangeText,
      parsedRecovery.detectedText,parsedInstallment.detectedText,counterparty,type.name());
  }

  private record ParsedDate(LocalDate expectedDate,Integer periodYear,Integer periodMonth,String detectedText,boolean invalidDate){}
  private ParsedDate parseDate(String raw,int defaultYear,int defaultMonth,List<String>w){
    Matcher m=Pattern.compile("\\b(\\d{1,2})[\\/\\-.](\\d{1,2})(?:[\\/\\-.](\\d{4}))?\\b").matcher(raw);
    if(!m.find()) return new ParsedDate(null,defaultYear,defaultMonth,null,false);
    int d=Integer.parseInt(m.group(1)), mon=Integer.parseInt(m.group(2)); int y=m.group(3)!=null?Integer.parseInt(m.group(3)):defaultYear;
    try{var ym=YearMonth.of(y,mon); if(d<1||d>ym.lengthOfMonth()) throw new IllegalArgumentException(); return new ParsedDate(LocalDate.of(y,mon,d),y,mon,m.group(),false);}
    catch(Exception ex){w.add("Fecha inválida detectada; se mantiene período por defecto."); return new ParsedDate(null,defaultYear,defaultMonth,m.group(),true);}
  }
  private record ParsedAmount(BigDecimal amount,BigDecimal minAmount,BigDecimal maxAmount,boolean hasAmount,String detectedAmountText,String detectedRangeText){}
  private ParsedAmount parseAmount(String raw,List<String>w,String installmentText){
    String l=raw.toLowerCase(Locale.ROOT);
    if(l.matches(".*\\b(sin monto|a confirmar|pendiente|reservar fecha)\\b.*")){w.add("Se detectó que el monto está pendiente de definir.");return new ParsedAmount(null,null,null,false,null,null);}    
    String cleaned=stripInstallments(stripDates(raw));
    ParsedRange range=findMoneyRange(cleaned);
    if(range!=null){ return new ParsedAmount(null,range.min,range.max,true,null,range.detectedText); }
    ParsedMoney single=findSingleMoney(cleaned);
    if(single!=null){ return new ParsedAmount(single.amount,null,null,true,single.detectedText,null);}
    return new ParsedAmount(null,null,null,false,null,null);
  }
  private BigDecimal toMoney(String t){ String n=normalizeMoneyToken(t); return n==null?null:new BigDecimal(n);}  

  private String normalizeMoneyToken(String token){
    if(token==null) return null;
    String c=token.replace("$","").trim();
    if(c.contains("/")) return null;
    c=c.replace(".","").replace(",","");
    if(!c.matches("\\d+")) return null;
    return c;
  }

  private record ParsedRange(BigDecimal min,BigDecimal max,String detectedText){}
  private record ParsedMoney(BigDecimal amount,String detectedText){}

  private String stripDates(String raw){
    return raw.replaceAll("\\b\\d{1,2}[\\/\\-.]\\d{1,2}(?:[\\/\\-.]\\d{4})?\\b"," ");
  }

  private String stripInstallments(String raw){
    return raw.replaceAll("(?i)\\b(?:cuota|cta)\\s*(?:nro\\s*)?\\d{1,2}\\s*(?:[/]|\\s+de\\s+)\\d{1,2}\\b|\\b\\d{1,2}\\s+de\\s+\\d{1,2}\\s+cuotas\\b"," ");
  }

  private ParsedRange findMoneyRange(String cleaned){
    Matcher range=Pattern.compile("(?i)(?:entre\\s+)?([$]?(?:\\d{1,3}(?:\\.\\d{3})+|\\d{5,}))(?:\\s*a\\s*|\\s*-\\s*|\\s+y\\s+)([$]?(?:\\d{1,3}(?:\\.\\d{3})+|\\d{5,}))").matcher(cleaned);
    if(range.find()){
      BigDecimal a=toMoney(range.group(1)),b=toMoney(range.group(2));
      if(a!=null&&b!=null){
        return new ParsedRange(a.min(b),a.max(b),range.group());
      }
    }
    return null;
  }

  private ParsedMoney findSingleMoney(String cleaned){
    Matcher m=Pattern.compile("\\$?\\d{1,3}(?:[.,]\\d{3})+|\\$?\\d{5,}").matcher(cleaned);
    if(m.find()){
      BigDecimal a=toMoney(m.group());
      if(a!=null) return new ParsedMoney(a,m.group());
    }
    return null;
  }

  private record ParsedInstallment(Integer number,Integer total,String detectedText){}
  private ParsedInstallment parseInstallment(String raw,List<String>w){
    String[] patterns={"(?i)\\b(?:cuota|cta)\\s*(?:nro\\s*)?(\\d{1,2})\\s*/\\s*(\\d{1,2})\\b","(?i)\\b(?:cuota|cta)\\s*(?:nro\\s*)?(\\d{1,2})\\s+de\\s+(\\d{1,2})\\b","(?i)\\b(\\d{1,2})\\s+de\\s+(\\d{1,2})\\s+cuotas\\b"};
    for(String p:patterns){ Matcher m=Pattern.compile(p).matcher(raw); if(m.find()){Integer n=Integer.parseInt(m.group(1)); Integer t=Integer.parseInt(m.group(2)); if(n>t) w.add("La cuota detectada parece inválida (número mayor al total)."); return new ParsedInstallment(n,t,m.group()); }}
    return new ParsedInstallment(null,null,null);
  }
  private record ParsedRecovery(BigDecimal recoveryAmount,BigDecimal recoveryPercent,String detectedText,String detectedCounterparty,boolean hasRecoverySignal){}
  private ParsedRecovery parseRecovery(String raw){
    Matcher p=Pattern.compile("(?i)\\b(?:recupero|recuperar|devuelve|devoluci[oó]n)?\\s*(\\d{1,3})%(?!\\w)").matcher(raw);
    if(p.find()){ return new ParsedRecovery(null,new BigDecimal(p.group(1)),p.group(),null,true);} 
    Matcher m=Pattern.compile("(?i)(recupero|recuperar|devuelve|devoluci[oó]n)\\s*([$]?\\d{1,3}(?:[.,]\\d{3})+|[$]?\\d{5,})").matcher(raw);
    if(m.find()) return new ParsedRecovery(toMoney(m.group(2)),null,m.group(),null,true);
    boolean signal=Pattern.compile("(?i)\\b(recupero|recuperar|devuelve|devoluci[oó]n)\\b").matcher(raw).find();
    return new ParsedRecovery(null,null,null,null,signal);
  }
  private String detectCounterparty(String raw,String fromRecovery){
    if(fromRecovery!=null) return cap(fromRecovery);
    Matcher m=Pattern.compile("(?i)\\b(agus|agustina|florencia|juliana|megu)\\b").matcher(raw);
    return m.find()?cap(m.group(1)):null;
  }
  private record TypeRes(MonthlyPlanItem.Type type,boolean weak){}
  private TypeRes inferType(String raw,boolean hasAmount,boolean hasRecoverySignal,List<String>w){String l=raw.toLowerCase(Locale.ROOT);
    boolean expenseHint=l.matches(".*\\b(pagar|pago|hostel|escuela|pasajes|gym|psic[oó]loga|psiquiatra|inmobiliaria|cejas|boxers|medias|bombachas|conjuntos|cooperadora|bici|inflables|perfilado)\\b.*");
    if(!hasAmount && l.matches(".*\\b(reservar|averiguar|definir|cotizar)\\b.*")) return new TypeRes(MonthlyPlanItem.Type.TODO,false);
    if(hasRecoverySignal){
      if(expenseHint){w.add("Se detectó recupero; revisá si corresponde cargarlo como gasto con recupero o como recupero independiente."); return new TypeRes(MonthlyPlanItem.Type.EXPENSE,false);} 
      w.add("Se detectó recupero; revisá si corresponde cargarlo como gasto con recupero o como recupero independiente.");
      return new TypeRes(MonthlyPlanItem.Type.RECOVERY,true);
    }
    if(l.matches(".*\\b(sueldo|ingreso|cobro|paga|transferencia recibida)\\b.*")) return new TypeRes(MonthlyPlanItem.Type.INCOME,false);
    if(expenseHint) return new TypeRes(MonthlyPlanItem.Type.EXPENSE,false);
    return new TypeRes(MonthlyPlanItem.Type.EXPENSE,true);
  }
  private MonthlyPlanItem.Priority inferPriority(String raw){String l=raw.toLowerCase(Locale.ROOT);
    if(l.matches(".*\\b(sueldo|escuela|alquiler|inmobiliaria|salud|psiquiatra|psic[oó]loga|pasajes|comida)\\b.*")) return MonthlyPlanItem.Priority.ESSENTIAL;
    if(l.matches(".*\\b(hostel|gym|cooperadora|bici|ropa hijas)\\b.*")) return MonthlyPlanItem.Priority.IMPORTANT;
    if(l.matches(".*\\b(cejas|inflables|ropa|extras)\\b.*")) return MonthlyPlanItem.Priority.OPTIONAL;
    return MonthlyPlanItem.Priority.IMPORTANT;
  }
  private String buildTitle(String raw){ String t=raw; t=t.replaceAll("\\b\\d{1,2}[\\/\\-.]\\d{1,2}(?:[\\/\\-.]\\d{4})?\\b"," "); t=t.replaceAll("(?i)\\b(cuota|cta)\\s*(?:nro\\s*)?\\d+\\s*(?:[/]|\\s+de\\s+)\\d+\\b|\\b\\d+\\s+de\\s+\\d+\\s+cuotas\\b"," "); t=t.replaceAll("(?i)\\b(recupero|recuperar|devuelve|devoluci[oó]n)\\b\\s*\\d+%?"," "); t=t.replaceAll("[$]?\\d{1,3}(?:[.,]\\d{3})*|[$]?\\d+"," "); t=t.replaceAll("(?i)\\b(sin monto|a confirmar|pendiente|reservar fecha)\\b"," "); t=t.replaceAll("\\s+"," ").trim(); return capWords(t); }
  private QuickCaptureConfidence confidenceOf(String title,MonthlyPlanItem.Type type,ParsedAmount amount,ParsedDate date,List<String>w){
    boolean invalidDate=w.stream().anyMatch(x->x.toLowerCase(Locale.ROOT).contains("fecha inválida")) || date.invalidDate;
    boolean richTitle=title!=null && title.trim().length()>=4;
    boolean strongType=type==MonthlyPlanItem.Type.EXPENSE||type==MonthlyPlanItem.Type.INCOME||type==MonthlyPlanItem.Type.RECOVERY;
    boolean hasPeriod=date.periodYear!=null&&date.periodMonth!=null;
    if(invalidDate || !richTitle) return QuickCaptureConfidence.LOW;
    if(strongType&&hasPeriod&&amount.hasAmount) return QuickCaptureConfidence.HIGH;
    if(strongType&&hasPeriod) return QuickCaptureConfidence.MEDIUM;
    return QuickCaptureConfidence.LOW;
  }
  private String cap(String s){ if(s==null||s.isBlank()) return s; return s.substring(0,1).toUpperCase()+s.substring(1).toLowerCase(); }
  private String capWords(String s){ StringBuilder b=new StringBuilder(); for(String p:s.split(" ")){ if(p.isBlank()) continue; if(!b.isEmpty()) b.append(" "); b.append(cap(p)); } return b.toString(); }
}
