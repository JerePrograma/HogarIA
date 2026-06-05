package com.hogaria.repository;

import com.hogaria.entity.ExcelImportRow;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExcelImportRowRepository extends JpaRepository<ExcelImportRow, UUID> {
  List<ExcelImportRow> findByBatchIdOrderByRowNumber(UUID batchId);

  @Query("""
          select row from ExcelImportRow row
          join ExcelImportBatch batch on batch.id = row.batchId
          where batch.profileId = :profileId
          order by batch.createdAt asc, row.rowNumber asc
          """)
  List<ExcelImportRow> findByProfileIdOrderByBatchAndRow(@Param("profileId") UUID profileId);
}
