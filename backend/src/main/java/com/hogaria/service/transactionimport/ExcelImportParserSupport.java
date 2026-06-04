package com.hogaria.service.transactionimport;

import com.hogaria.entity.ImportTargetEntity;
import com.hogaria.entity.MoneyTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;

abstract class ExcelImportParserSupport {
  protected static final Locale ES_AR = new Locale("es", "AR");
  protected static final String DEFAULT_CURRENCY = "ARS";

  protected final ImportTextNormalizer normalizer;

  protected ExcelImportParserSupport(ImportTextNormalizer normalizer) {
    this.normalizer = normalizer;
  }

  protected List<String> readRow(
          org.apache.poi.ss.usermodel.Row row,
          DataFormatter formatter,
          int expectedColumns
  ) {
    var values = new ArrayList<String>();

    if (row == null) {
      return values;
    }

    var lastCell = Math.max(expectedColumns, row.getLastCellNum());

    for (int i = 0; i < lastCell; i++) {
      values.add(cell(row, i, formatter));
    }

    return values;
  }

  protected String value(List<String> values, Map<String, Integer> indexes, String header) {
    var index = indexes.get(normalizer.canonicalHeader(header));

    if (index == null || index < 0 || index >= values.size()) {
      return "";
    }

    return normalizer.cleanRaw(values.get(index));
  }

  protected String firstNonBlank(String... values) {
    if (values == null) {
      return "";
    }

    for (var value : values) {
      if (value != null && !value.trim().isBlank()) {
        return value.trim();
      }
    }

    return "";
  }

  protected boolean isBlankRow(List<String> values) {
    if (values == null || values.isEmpty()) {
      return true;
    }

    for (var value : values) {
      if (value != null && !normalizer.cleanRaw(value).isBlank()) {
        return false;
      }
    }

    return true;
  }

  protected BigDecimal parseAmount(String value) {
    var clean = normalizer.cleanRaw(value)
            .replace("$", "")
            .replace(" ", "")
            .trim();

    if (clean.isBlank()) {
      throw new IllegalArgumentException("Importe vacío.");
    }

    boolean negativeByParentheses = clean.startsWith("(") && clean.endsWith(")");

    if (negativeByParentheses) {
      clean = clean.substring(1, clean.length() - 1);
    }

    clean = clean.replace("+", "");

    if (clean.contains(",") && clean.contains(".")) {
      var lastComma = clean.lastIndexOf(',');
      var lastDot = clean.lastIndexOf('.');

      if (lastComma > lastDot) {
        clean = clean.replace(".", "").replace(",", ".");
      } else {
        clean = clean.replace(",", "");
      }
    } else if (clean.contains(",")) {
      clean = clean.replace(".", "").replace(",", ".");
    }

    var amount = new BigDecimal(clean);
    return negativeByParentheses ? amount.negate() : amount;
  }

  protected LocalDate parseBancoProvinciaDate(String value) {
    var clean = normalizer.cleanRaw(value);
    var formatters = List.of(
            DateTimeFormatter.ofPattern("d/M/uuuu", ES_AR),
            DateTimeFormatter.ofPattern("dd/MM/uuuu", ES_AR),
            DateTimeFormatter.ISO_LOCAL_DATE
    );

    for (var formatter : formatters) {
      try {
        return LocalDate.parse(clean, formatter);
      } catch (DateTimeParseException ignored) {
      }
    }

    throw new IllegalArgumentException("Fecha Banco Provincia inválida: " + value);
  }

  protected Map<String, String> rawMap(
          List<String> headers,
          List<String> values,
          String detectedFormat,
          String sheetName,
          int rowNumber
  ) {
    var raw = new LinkedHashMap<String, String>();

    for (int i = 0; i < headers.size(); i++) {
      raw.put(headers.get(i), i < values.size() ? normalizer.cleanRaw(values.get(i)) : "");
    }

    raw.put("_detectedFormat", detectedFormat);
    raw.put("_sheetName", sheetName);
    raw.put("_rowNumber", String.valueOf(rowNumber));

    return raw;
  }

  protected ImportTargetEntity targetEntity(MoneyTransaction.MovementType movementType) {
    if (movementType == null) {
      return ImportTargetEntity.UNKNOWN;
    }

    return switch (movementType) {
      case INCOME -> ImportTargetEntity.INCOME;
      case EXPENSE -> ImportTargetEntity.EXPENSE;
      case SAVING -> ImportTargetEntity.SAVING;
      default -> ImportTargetEntity.UNKNOWN;
    };
  }

  protected String joinUseful(String... values) {
    var parts = new ArrayList<String>();

    if (values != null) {
      for (var value : values) {
        var clean = normalizer.cleanRaw(value);

        if (!clean.isBlank() && !parts.contains(clean)) {
          parts.add(clean);
        }
      }
    }

    return String.join(" | ", parts);
  }

  protected String extractCounterparty(String extendedDescription, String merchantName) {
    var merchant = normalizer.cleanRaw(merchantName);

    if (!merchant.isBlank()) {
      return merchant;
    }

    var extended = normalizer.cleanRaw(extendedDescription);

    if (extended.isBlank()) {
      return "";
    }

    var nameMatcher = Pattern
            .compile("\\bN:([^\\r\\n]+?)(?:\\s+(?:CA:|C:|D:|ID\\.|REF\\.|VAR\\b)|$)", Pattern.CASE_INSENSITIVE)
            .matcher(extended);

    if (nameMatcher.find()) {
      return normalizer.cleanRaw(nameMatcher.group(1));
    }

    var parenthesisMatcher = Pattern.compile("\\((\\d{7,})\\)").matcher(extended);

    if (parenthesisMatcher.find()) {
      return parenthesisMatcher.group(1);
    }

    var cuitMatcher = Pattern.compile("\\b[CD]:\\s*(\\d{7,})\\b", Pattern.CASE_INSENSITIVE).matcher(extended);

    if (cuitMatcher.find()) {
      return cuitMatcher.group(1);
    }

    return "";
  }

  private String cell(org.apache.poi.ss.usermodel.Row row, int index, DataFormatter formatter) {
    if (row == null || index < 0) {
      return "";
    }

    var cell = row.getCell(index);

    if (cell == null) {
      return "";
    }

    if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
      return cell.getLocalDateTimeCellValue().toLocalDate().toString();
    }

    return normalizer.cleanRaw(formatter.formatCellValue(cell));
  }
}
