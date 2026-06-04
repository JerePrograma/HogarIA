package com.hogaria.service.transactionimport;

import com.hogaria.exception.BadRequestException;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

@Component
public class TransactionExcelImportFormatDetector {

  private static final Locale ES_AR = new Locale("es", "AR");
  private static final int MAX_HEADER_SCAN_ROWS = 10;

  private final ImportTextNormalizer normalizer;

  public TransactionExcelImportFormatDetector(ImportTextNormalizer normalizer) {
    this.normalizer = normalizer;
  }

  public DetectedExcelImportFormat detect(byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      throw new BadRequestException("El archivo de importación está vacío.");
    }

    var formatter = new DataFormatter(ES_AR);
    var scannedHeaders = new ArrayList<String>();

    try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
      for (var sheet : workbook) {
        int lastRow = Math.min(sheet.getLastRowNum(), MAX_HEADER_SCAN_ROWS - 1);

        for (int rowIndex = 0; rowIndex <= lastRow; rowIndex++) {
          var row = sheet.getRow(rowIndex);
          var values = readRow(row, formatter);

          if (values.isEmpty()) {
            continue;
          }

          scannedHeaders.add(
                  "hoja '" + sheet.getSheetName() + "' fila " + (rowIndex + 1)
                          + ": " + String.join(" | ", values)
          );

          var indexes = buildHeaderIndex(values);

          if (isMercadoPagoSettlement(indexes)) {
            return new DetectedExcelImportFormat(
                    ExcelImportTemplate.MERCADO_PAGO_SETTLEMENT,
                    sheet.getSheetName(),
                    rowIndex,
                    values,
                    indexes
            );
          }

          if (isBancoProvinciaMovimientos(indexes)) {
            return new DetectedExcelImportFormat(
                    ExcelImportTemplate.BANCO_PROVINCIA_MOVIMIENTOS,
                    sheet.getSheetName(),
                    rowIndex,
                    values,
                    indexes
            );
          }
        }
      }
    } catch (BadRequestException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BadRequestException("No se pudo leer el Excel: " + ex.getMessage());
    }

    throw new BadRequestException(
            "No se pudo detectar una plantilla soportada. "
                    + "Formatos esperados: MERCADO_PAGO_SETTLEMENT o BANCO_PROVINCIA_MOVIMIENTOS. "
                    + "Headers encontrados: " + String.join(" || ", scannedHeaders)
    );
  }

  public Map<String, Integer> buildHeaderIndex(List<String> headers) {
    var indexes = new LinkedHashMap<String, Integer>();

    for (int i = 0; i < headers.size(); i++) {
      var key = normalizer.canonicalHeader(headers.get(i));

      if (!key.isBlank() && !indexes.containsKey(key)) {
        indexes.put(key, i);
      }
    }

    return indexes;
  }

  private boolean isMercadoPagoSettlement(Map<String, Integer> indexes) {
    return has(indexes, "fecha de origen")
            && has(indexes, "fecha de aprobacion")
            && has(indexes, "id de operacion en mercado pago")
            && has(indexes, "tipo de operacion")
            && has(indexes, "monto neto de la operacion que impacto tu dinero")
            && has(indexes, "detalle de la venta");
  }

  private boolean isBancoProvinciaMovimientos(Map<String, Integer> indexes) {
    return has(indexes, "numero secuencia")
            && has(indexes, "fecha")
            && has(indexes, "importe")
            && has(indexes, "saldo")
            && has(indexes, "descripcion")
            && has(indexes, "descripcion extendida")
            && has(indexes, "nombre comercio");
  }

  private boolean has(Map<String, Integer> indexes, String header) {
    return indexes.containsKey(normalizer.canonicalHeader(header));
  }

  private List<String> readRow(org.apache.poi.ss.usermodel.Row row, DataFormatter formatter) {
    if (row == null || row.getFirstCellNum() < 0 || row.getLastCellNum() < 0) {
      return List.of();
    }

    var values = new ArrayList<String>();

    for (int i = 0; i < row.getLastCellNum(); i++) {
      var cell = row.getCell(i);
      values.add(cell == null ? "" : normalizer.cleanRaw(formatter.formatCellValue(cell)));
    }

    while (!values.isEmpty() && values.get(values.size() - 1).isBlank()) {
      values.remove(values.size() - 1);
    }

    return values;
  }
}
