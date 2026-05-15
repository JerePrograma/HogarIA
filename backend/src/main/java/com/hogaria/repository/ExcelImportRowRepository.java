package com.hogaria.repository;

import com.hogaria.entity.ExcelImportRow;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExcelImportRowRepository extends JpaRepository<ExcelImportRow, UUID> {
  List<ExcelImportRow> findByBatchIdOrderByRowNumber(UUID batchId);
}
