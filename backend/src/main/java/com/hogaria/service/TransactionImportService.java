package com.hogaria.service;

import com.hogaria.dto.TransactionImportDtos.TransactionImportCommitRequest;
import com.hogaria.dto.TransactionImportDtos.TransactionImportCommitResponse;
import com.hogaria.dto.TransactionImportDtos.TransactionImportPreviewResponse;
import com.hogaria.dto.TransactionImportDtos.TransactionImportSource;
import com.hogaria.service.transactionimport.TransactionImportCommitService;
import com.hogaria.service.transactionimport.TransactionImportPreviewService;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TransactionImportService {

  private final TransactionImportPreviewService previewService;
  private final TransactionImportCommitService commitService;

  public TransactionImportService(
          TransactionImportPreviewService previewService,
          TransactionImportCommitService commitService
  ) {
    this.previewService = previewService;
    this.commitService = commitService;
  }

  @Transactional
  public TransactionImportPreviewResponse preview(
          UUID userId,
          UUID profileId,
          UUID accountId,
          TransactionImportSource source,
          MultipartFile file,
          Integer year,
          Integer month
  ) {
    return previewService.preview(userId, profileId, accountId, source, file, year, month);
  }

  @Transactional(readOnly = true)
  public TransactionImportPreviewResponse getBatch(UUID userId, UUID profileId, UUID batchId) {
    return previewService.getBatch(userId, profileId, batchId);
  }

  @Transactional
  public TransactionImportCommitResponse commit(
          UUID userId,
          UUID profileId,
          UUID batchId,
          TransactionImportCommitRequest request
  ) {
    return commitService.commit(userId, profileId, batchId, request);
  }
}