package com.hogaria.service.transactionimport;

import java.util.List;
import java.util.UUID;

public interface TransactionExcelMovementParser {
  ExcelImportTemplate template();

  List<ImportedMovementCandidate> parse(
          byte[] bytes,
          DetectedExcelImportFormat detection,
          UUID profileId,
          UUID accountId
  );
}
