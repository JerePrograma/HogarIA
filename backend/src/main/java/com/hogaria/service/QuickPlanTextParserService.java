package com.hogaria.service;

import com.hogaria.dto.QuickPlanTextDtos.AmountScale;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.regex.*;
import org.springframework.stereotype.Service;

@Service
public class QuickPlanTextParserService {
  public record ParsedLine(String title, BigDecimal amount, BigDecimal minAmount, BigDecimal maxAmount, boolean approximate) {}

  public ParsedLine parseLine(String line, AmountScale scale, BigDecimal margin) {
    Matcher m = Pattern.compile("(?i)(\\$?\\s*\\d[\\d.,]*\\s*(?:mil|k|m)?)").matcher(line);
    if (!m.find()) return new ParsedLine(cleanTitle(line), null, null, null, false);
    BigDecimal amount = parseAmountToken(m.group(1));
    if (amount == null) return new ParsedLine(cleanTitle(line), null, null, null, false);
    if ((scale == null || scale == AmountScale.THOUSANDS) && amount.scale() <= 0 && amount.compareTo(new BigDecimal("10000")) < 0) amount = amount.multiply(new BigDecimal("1000"));
    boolean approx = line.toLowerCase(Locale.ROOT).matches(".*(aprox|como|~|si o si|urgente).*?");
    String title = cleanTitle(line.replace(m.group(1), ""));
    if (approx) {
      BigDecimal pct = margin == null ? new BigDecimal("0.20") : margin;
      BigDecimal delta = amount.multiply(pct).setScale(2, RoundingMode.HALF_UP);
      return new ParsedLine(title, null, amount.subtract(delta), amount.add(delta), true);
    }
    return new ParsedLine(title, amount.setScale(2, RoundingMode.HALF_UP), null, null, false);
  }

  private BigDecimal parseAmountToken(String raw) {
    String token = raw.toLowerCase(Locale.ROOT).replace("$", "").replaceAll("\\s+", "");
    BigDecimal mult = BigDecimal.ONE;
    if (token.endsWith("mil") || token.endsWith("k")) { mult = new BigDecimal("1000"); token = token.replaceAll("(mil|k)$", ""); }
    else if (token.endsWith("m")) { mult = new BigDecimal("1000000"); token = token.substring(0, token.length()-1); }
    if (token.contains(",") && token.contains(".")) {
      if (token.lastIndexOf(',') > token.lastIndexOf('.')) token = token.replace(".", "").replace(',', '.');
      else token = token.replace(",", "");
    } else if (token.contains(",")) {
      token = token.matches(".*\\d,\\d{1,2}$") ? token.replace(',', '.') : token.replace(",", "");
    } else if (token.contains(".")) {
      token = token.matches(".*\\d\\.\\d{1,2}$") ? token : token.replace(".", "");
    }
    try { return new BigDecimal(token).multiply(mult); } catch (Exception e) { return null; }
  }

  private String cleanTitle(String t) {
    return t.replaceAll("(?i)\\b(aprox|como|si o si|urgente|mil|k|m)\\b", " ")
        .replace("~", " ")
        .replaceAll("\\d+[\\d.,]*", " ")
        .replaceAll("\\s+", " ").replaceAll("^[*\\-\\s]+|[*\\-\\s]+$", "").trim();
  }
}
