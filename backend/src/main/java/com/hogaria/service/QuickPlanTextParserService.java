package com.hogaria.service;

import com.hogaria.dto.QuickPlanTextDtos.AmountScale;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Locale;
import java.util.regex.*;
import org.springframework.stereotype.Service;

@Service
public class QuickPlanTextParserService {
  public record ParsedLine(
      String title,
      BigDecimal amount,
      BigDecimal minAmount,
      BigDecimal maxAmount,
      boolean approximate,
      LocalDate expectedDate,
      String detectedDateText,
      boolean invalidDate) {}

  public ParsedLine parseLine(String line, AmountScale scale, BigDecimal margin) {
    return parseLine(line, scale, margin, null, null);
  }

  public ParsedLine parseLine(String line, AmountScale scale, BigDecimal margin, Integer defaultYear, Integer defaultMonth) {
    DateDetection date = detectDate(line, defaultYear);
    String analyzableLine = date.detectedText() == null ? line : line.replace(date.detectedText(), " ");
    Matcher m = Pattern.compile("(?i)(\\$?\\s*\\d[\\d.,]*\\s*(?:mil|k|m)?)").matcher(analyzableLine);
    if (!m.find()) return new ParsedLine(cleanTitle(analyzableLine), null, null, null, false, date.expectedDate(), date.detectedText(), date.invalid());
    BigDecimal amount = parseAmountToken(m.group(1));
    if (amount == null) return new ParsedLine(cleanTitle(analyzableLine), null, null, null, false, date.expectedDate(), date.detectedText(), date.invalid());
    if ((scale == null || scale == AmountScale.THOUSANDS) && amount.scale() <= 0 && amount.compareTo(new BigDecimal("10000")) < 0) amount = amount.multiply(new BigDecimal("1000"));
    boolean approx = analyzableLine.toLowerCase(Locale.ROOT).matches(".*(aprox|como|~|si o si|urgente).*?");
    String title = cleanTitle(analyzableLine.replace(m.group(1), ""));
    if (approx) {
      BigDecimal pct = margin == null ? new BigDecimal("0.20") : margin;
      BigDecimal delta = amount.multiply(pct).setScale(2, RoundingMode.HALF_UP);
      return new ParsedLine(title, null, amount.subtract(delta), amount.add(delta), true, date.expectedDate(), date.detectedText(), date.invalid());
    }
    return new ParsedLine(title, amount.setScale(2, RoundingMode.HALF_UP), null, null, false, date.expectedDate(), date.detectedText(), date.invalid());
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
    return t.replaceAll("\\b\\d{1,2}[\\/\\-.]\\d{1,2}(?:[\\/\\-.]\\d{2,4})?\\b", " ")
        .replaceAll("(?i)\\b(?:cuota|cta)\\s*(?:nro\\s*)?\\d{1,2}\\s*(?:[/]|\\s+de\\s+)\\d{1,2}\\b", " ")
        .replaceAll("(?i)\\b(aprox|como|si o si|urgente|mil|k|m)\\b", " ")
        .replace("~", " ")
        .replaceAll("\\d+[\\d.,]*", " ")
        .replaceAll("\\s+", " ").replaceAll("^[*\\-\\s]+|[*\\-\\s]+$", "").trim();
  }

  private record DateDetection(LocalDate expectedDate, String detectedText, boolean invalid) {}

  private DateDetection detectDate(String line, Integer defaultYear) {
    Matcher m = Pattern.compile("\\b(\\d{1,2})[\\/\\-.](\\d{1,2})(?:[\\/\\-.](\\d{2}|\\d{4}))?\\b").matcher(line);
    while (m.find()) {
      if (looksLikeInstallment(line, m.start(), m.end())) {
        continue;
      }

      int day = Integer.parseInt(m.group(1));
      int month = Integer.parseInt(m.group(2));
      int year = resolveYear(m.group(3), defaultYear);

      try {
        YearMonth ym = YearMonth.of(year, month);
        if (day < 1 || day > ym.lengthOfMonth()) {
          throw new IllegalArgumentException();
        }

        return new DateDetection(LocalDate.of(year, month, day), m.group(), false);
      } catch (RuntimeException ex) {
        return new DateDetection(null, m.group(), true);
      }
    }

    return new DateDetection(null, null, false);
  }

  private int resolveYear(String rawYear, Integer defaultYear) {
    if (rawYear == null || rawYear.isBlank()) {
      return defaultYear == null ? LocalDate.now().getYear() : defaultYear;
    }

    int year = Integer.parseInt(rawYear);
    return rawYear.length() == 2 ? 2000 + year : year;
  }

  private boolean looksLikeInstallment(String line, int start, int end) {
    String prefix = line.substring(Math.max(0, start - 16), start).toLowerCase(Locale.ROOT);
    String suffix = line.substring(end, Math.min(line.length(), end + 12)).toLowerCase(Locale.ROOT);
    return prefix.matches(".*\\b(cuota|cta)(?:\\s+nro)?\\s*$") || suffix.matches("^\\s*(cuota|cuotas)\\b.*");
  }
}
