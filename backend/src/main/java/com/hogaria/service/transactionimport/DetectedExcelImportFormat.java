package com.hogaria.service.transactionimport;

import java.util.List;
import java.util.Map;

public record DetectedExcelImportFormat(
        ExcelImportTemplate template,
        String sheetName,
        int headerRowIndex,
        List<String> headers,
        Map<String, Integer> headerIndexes
) {
  public String displayName() {
    return template().name();
  }

  public int headerRowNumber() {
    return headerRowIndex() + 1;
  }
}
