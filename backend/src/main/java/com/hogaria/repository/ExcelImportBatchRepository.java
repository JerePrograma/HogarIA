package com.hogaria.repository;

import com.hogaria.entity.ExcelImportBatch;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExcelImportBatchRepository extends JpaRepository<ExcelImportBatch, UUID> {}
