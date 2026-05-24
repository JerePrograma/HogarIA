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
    Matcher m = Pattern.compile("(\\d+[\\d.,]*)").matcher(line);
    if (!m.find()) return new ParsedLine(cleanTitle(line), null, null, null, false);
    BigDecimal amount = parseNumber(m.group(1));
    if (amount == null) return new ParsedLine(cleanTitle(line), null, null, null, false);
    if ((scale == null || scale == AmountScale.THOUSANDS) && amount.scale() <= 0 && amount.compareTo(new BigDecimal("10000")) < 0) amount = amount.multiply(new BigDecimal("1000"));
    boolean approx = line.toLowerCase(Locale.ROOT).matches(".*(aprox|como|~).*?");
    String title = cleanTitle(line.replace(m.group(1), ""));
    if (approx) {
      BigDecimal pct = margin == null ? new BigDecimal("0.20") : margin;
      BigDecimal delta = amount.multiply(pct).setScale(2, RoundingMode.HALF_UP);
      return new ParsedLine(title, null, amount.subtract(delta), amount.add(delta), true);
    }
    return new ParsedLine(title, amount.setScale(2, RoundingMode.HALF_UP), null, null, false);
  }

  private BigDecimal parseNumber(String token) {
    String n = token.replace(".", "").replace(",", "");
    if (!n.matches("\\d+")) return null;
    return new BigDecimal(n);
  }

  private String cleanTitle(String t) {
    return t.replaceAll("\\s+", " ").replaceAll("^[*\\-\\s]+|[*\\-\\s]+$", "").trim();
  }
}
