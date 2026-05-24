package com.hogaria.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.text.Normalizer;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BancoProvinciaLoanExcelParserService {
  public record ParsedLoanRow(Integer lineNumber, String tipo, String identificacion, String accountNumber, BigDecimal currentDebtAmount, BigDecimal originalAmount, LocalDate dueDate) {}

  public List<ParsedLoanRow> parse(MultipartFile file) {
    try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
      Sheet sheet = workbook.getSheetAt(0);
      DataFormatter fmt = new DataFormatter(Locale.forLanguageTag("es-AR"));
      int headerRowIndex = -1;
      Map<String, Integer> headers = Map.of();
      for (int i = 0; i <= sheet.getLastRowNum(); i++) {
        var map = extractHeaders(sheet.getRow(i), fmt);
        if (map.keySet().containsAll(List.of("tipo", "identificacion", "numero de cuenta", "deuda a la fecha", "importe original", "vencimiento"))) {
          headerRowIndex = i;
          headers = map;
          break;
        }
      }
      if (headerRowIndex < 0) throw new IllegalArgumentException("No se encontró cabecera de Banco Provincia Mis Préstamos.");
      List<ParsedLoanRow> rows = new ArrayList<>();
      for (int i = headerRowIndex + 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null) continue;
        String account = cell(row, headers.get("numero de cuenta"), fmt);
        if (account.isBlank()) continue;
        rows.add(new ParsedLoanRow(i + 1, cell(row, headers.get("tipo"), fmt), cell(row, headers.get("identificacion"), fmt), account, parseAmount(cell(row, headers.get("deuda a la fecha"), fmt)), parseAmount(cell(row, headers.get("importe original"), fmt)), parseDate(cell(row, headers.get("vencimiento"), fmt))));
      }
      return rows;
    } catch (IOException e) {
      throw new IllegalArgumentException("No se pudo leer el archivo Excel.", e);
    }
  }

  private Map<String, Integer> extractHeaders(Row row, DataFormatter formatter) {
    if (row == null) return Map.of();
    Map<String, Integer> map = new HashMap<>();
    for (int c = row.getFirstCellNum(); c <= row.getLastCellNum(); c++) {
      if (c < 0) continue;
      String value = normalizeHeader(cell(row, c, formatter));
      if (!value.isBlank()) map.put(value, c);
    }
    return map;
  }

  private String normalizeHeader(String value) { if (value == null) return ""; String n = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", ""); return n.toLowerCase().trim(); }
  private String cell(Row row, Integer index, DataFormatter formatter) { if (index == null || row == null) return ""; Cell cell = row.getCell(index); if (cell == null) return ""; return formatter.formatCellValue(cell).trim(); }
  private BigDecimal parseAmount(String value) { String clean = value.replace("$", "").replace(" ", "").trim(); if (clean.contains(",")) clean = clean.replace(".", "").replace(",", "."); return clean.isBlank() ? BigDecimal.ZERO : new BigDecimal(clean); }
  private LocalDate parseDate(String value) { return LocalDate.parse(value.trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy")); }
}
