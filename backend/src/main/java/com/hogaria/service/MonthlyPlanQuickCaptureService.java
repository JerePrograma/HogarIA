package com.hogaria.service;

import com.hogaria.dto.MonthlyPlanDtos.MonthlyPlanItemCreateRequest;
import com.hogaria.dto.QuickCaptureDtos.*;
import com.hogaria.entity.MonthlyPlanItem;
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
    profiles.findByIdAndUserId(profileId,userId).orElseThrow(()->new RuntimeException("Profile does not belong to user"));
    String raw=request.rawText()==null?"":request.rawText().trim();
    List<String> warnings=new ArrayList<>();
    Integer defaultYear=request.defaultYear()!=null?request.defaultYear():LocalDate.now().getYear();
    Integer defaultMonth=request.defaultMonth()!=null?request.defaultMonth():LocalDate.now().getMonthValue();
    if(defaultMonth<1||defaultMonth>12){defaultMonth=LocalDate.now().getMonthValue();warnings.add("Mes por defecto inválido; se usará el mes actual.");}

    ParsedDate parsedDate=parseDate(raw,defaultYear,defaultMonth,warnings);
    ParsedAmount parsedAmount=parseAmount(raw,warnings);
    ParsedInstallment parsedInstallment=parseInstallment(raw,warnings);
    ParsedRecovery parsedRecovery=parseRecovery(raw);
    String counterparty=detectCounterparty(raw, parsedRecovery.detectedCounterparty());
    var typeRes=inferType(raw, parsedAmount.hasAmount);
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

  private record ParsedDate(LocalDate expectedDate,Integer periodYear,Integer periodMonth,String detectedText){}
  private ParsedDate parseDate(String raw,int defaultYear,int defaultMonth,List<String>w){
    Matcher m=Pattern.compile("\\b(\\d{1,2})[\\/\\-.](\\d{1,2})(?:[\\/\\-.](\\d{4}))?\\b").matcher(raw);
    if(!m.find()) return new ParsedDate(null,defaultYear,defaultMonth,null);
    int d=Integer.parseInt(m.group(1)), mon=Integer.parseInt(m.group(2)); int y=m.group(3)!=null?Integer.parseInt(m.group(3)):defaultYear;
    try{var ym=YearMonth.of(y,mon); if(d<1||d>ym.lengthOfMonth()) throw new IllegalArgumentException(); return new ParsedDate(LocalDate.of(y,mon,d),y,mon,m.group());}
    catch(Exception ex){w.add("Fecha inválida detectada; se mantiene período por defecto."); return new ParsedDate(null,defaultYear,defaultMonth,m.group());}
  }
  private record ParsedAmount(BigDecimal amount,BigDecimal minAmount,BigDecimal maxAmount,boolean hasAmount,String detectedAmountText,String detectedRangeText){}
  private ParsedAmount parseAmount(String raw,List<String>w){
    String l=raw.toLowerCase(Locale.ROOT);
    if(l.matches(".*\\b(sin monto|a confirmar|pendiente|reservar fecha)\\b.*")){w.add("Se detectó que el monto está pendiente de definir.");return new ParsedAmount(null,null,null,false,null,null);}    
    Matcher range=Pattern.compile("(?i)(?:entre\\s+)?([$]?\\d{1,3}(?:[.,]\\d{3})*|[$]?\\d+)(?:\\s*a\\s*|\\s*-\\s*|\\s+y\\s+)([$]?\\d{1,3}(?:[.,]\\d{3})*|[$]?\\d+)").matcher(raw);
    if(range.find() && !range.group().contains("/")){
      BigDecimal a=toMoney(range.group(1)),b=toMoney(range.group(2));
      if(a!=null&&b!=null) return new ParsedAmount(null,a.min(b),a.max(b),true,null,range.group());
    }
    Matcher m=Pattern.compile("(?<!\\d[\\/\\-.])\\$?\\d{1,3}(?:[.,]\\d{3})+|(?<!\\d[\\/\\-.])\\$?\\d{5,}(?![\\/\\-.]\\d)").matcher(raw);
    if(m.find()){BigDecimal a=toMoney(m.group());return new ParsedAmount(a,null,null,a!=null,m.group(),null);} 
    return new ParsedAmount(null,null,null,false,null,null);
  }
  private BigDecimal toMoney(String t){if(t==null)return null; String c=t.replace("$","").replace(".","").replace(",","").trim(); if(!c.matches("\\d+")) return null; return new BigDecimal(c);}  
  private record ParsedInstallment(Integer number,Integer total,String detectedText){}
  private ParsedInstallment parseInstallment(String raw,List<String>w){
    Matcher m=Pattern.compile("(?i)(?:cuota|cta)?\\s*(\\d{1,2})\\s*[/]\\s*(\\d{1,2})|(?i)(\\d{1,2})\\s+de\\s+(\\d{1,2})").matcher(raw);
    if(!m.find()) return new ParsedInstallment(null,null,null);
    Integer n=m.group(1)!=null?Integer.parseInt(m.group(1)):Integer.parseInt(m.group(3));
    Integer t=m.group(2)!=null?Integer.parseInt(m.group(2)):Integer.parseInt(m.group(4));
    if(n>t) w.add("La cuota detectada parece inválida (número mayor al total).");
    return new ParsedInstallment(n,t,m.group());
  }
  private record ParsedRecovery(BigDecimal recoveryAmount,BigDecimal recoveryPercent,String detectedText,String detectedCounterparty){}
  private ParsedRecovery parseRecovery(String raw){
    Matcher p=Pattern.compile("(?i)(recupero|recuperar|devuelve|devoluci[oó]n)\\s*(\\d{1,3})%|\\b(agus|agustina|florencia|juliana)\\b\\s*(\\d{1,3})%").matcher(raw);
    if(p.find()){String pct=p.group(2)!=null?p.group(2):p.group(4); String cp=p.group(3); return new ParsedRecovery(null,new BigDecimal(pct),p.group(),cp);} 
    Matcher m=Pattern.compile("(?i)(recupero|recuperar|devuelve|devoluci[oó]n)\\s*([$]?\\d{1,3}(?:[.,]\\d{3})+|[$]?\\d{5,})").matcher(raw);
    if(m.find()) return new ParsedRecovery(toMoney(m.group(2)),null,m.group(),null);
    return new ParsedRecovery(null,null,null,null);
  }
  private String detectCounterparty(String raw,String fromRecovery){
    if(fromRecovery!=null) return cap(fromRecovery);
    Matcher m=Pattern.compile("(?i)\\b(agus|agustina|florencia|juliana|megu)\\b").matcher(raw);
    return m.find()?cap(m.group(1)):null;
  }
  private record TypeRes(MonthlyPlanItem.Type type,boolean weak){}
  private TypeRes inferType(String raw,boolean hasAmount){String l=raw.toLowerCase(Locale.ROOT);
    if(!hasAmount && l.matches(".*\\b(reservar|averiguar|definir|cotizar)\\b.*")) return new TypeRes(MonthlyPlanItem.Type.TODO,false);
    if(l.matches(".*\\b(recupero|recuperar|devuelve|devoluci[oó]n)\\b.*")) return new TypeRes(MonthlyPlanItem.Type.RECOVERY,false);
    if(l.matches(".*\\b(sueldo|ingreso|cobro|paga|transferencia recibida)\\b.*")) return new TypeRes(MonthlyPlanItem.Type.INCOME,false);
    if(l.matches(".*\\b(pagar|pago|hostel|escuela|pasajes|gym|psic[oó]loga|psiquiatra|inmobiliaria|cejas|boxers|medias|bombachas|conjuntos|cooperadora|bici|inflables)\\b.*")) return new TypeRes(MonthlyPlanItem.Type.EXPENSE,false);
    return new TypeRes(MonthlyPlanItem.Type.EXPENSE,true);
  }
  private MonthlyPlanItem.Priority inferPriority(String raw){String l=raw.toLowerCase(Locale.ROOT);
    if(l.matches(".*\\b(sueldo|escuela|alquiler|inmobiliaria|salud|psiquiatra|psic[oó]loga|pasajes|comida)\\b.*")) return MonthlyPlanItem.Priority.ESSENTIAL;
    if(l.matches(".*\\b(hostel|gym|cooperadora|bici|ropa hijas)\\b.*")) return MonthlyPlanItem.Priority.IMPORTANT;
    if(l.matches(".*\\b(cejas|inflables|ropa|extras)\\b.*")) return MonthlyPlanItem.Priority.OPTIONAL;
    return MonthlyPlanItem.Priority.IMPORTANT;
  }
  private String buildTitle(String raw){ String t=raw; t=t.replaceAll("\\b\\d{1,2}[\\/\\-.]\\d{1,2}(?:[\\/\\-.]\\d{4})?\\b"," "); t=t.replaceAll("(?i)\\b(cuota|cta)\\s*\\d+\\s*[/]\\s*\\d+\\b|\\b\\d+\\s+de\\s+\\d+\\b"," "); t=t.replaceAll("(?i)\\b(recupero|recuperar|devuelve|devoluci[oó]n)\\b\\s*\\d+%?"," "); t=t.replaceAll("[$]?\\d{1,3}(?:[.,]\\d{3})*|[$]?\\d+"," "); t=t.replaceAll("(?i)\\b(sin monto|a confirmar|pendiente|reservar fecha)\\b"," "); t=t.replaceAll("\\s+"," ").trim(); return capWords(t); }
  private QuickCaptureConfidence confidenceOf(String title,MonthlyPlanItem.Type type,ParsedAmount amount,ParsedDate date,List<String>w){
    if(!w.isEmpty() && w.stream().anyMatch(x->x.toLowerCase(Locale.ROOT).contains("inválida"))) return QuickCaptureConfidence.LOW;
    boolean richTitle=title!=null && title.trim().length()>=3;
    boolean hasPeriod=date.periodYear!=null&&date.periodMonth!=null;
    if(richTitle&&type!=null&&amount.hasAmount&&hasPeriod) return QuickCaptureConfidence.HIGH;
    if(richTitle&&type!=null&&hasPeriod) return QuickCaptureConfidence.MEDIUM;
    return QuickCaptureConfidence.LOW;
  }
  private String cap(String s){ if(s==null||s.isBlank()) return s; return s.substring(0,1).toUpperCase()+s.substring(1).toLowerCase(); }
  private String capWords(String s){ StringBuilder b=new StringBuilder(); for(String p:s.split(" ")){ if(p.isBlank()) continue; if(!b.isEmpty()) b.append(" "); b.append(cap(p)); } return b.toString(); }
}
