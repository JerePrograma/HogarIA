package com.hogaria.controller;

import com.hogaria.dto.TransactionImportDtos.TransactionImportCommitRequest;
import com.hogaria.dto.TransactionImportDtos.TransactionImportCommitResponse;
import com.hogaria.dto.TransactionImportDtos.TransactionImportPreviewResponse;
import com.hogaria.dto.TransactionImportDtos.TransactionImportSource;
import com.hogaria.security.CurrentUserResolver;
import com.hogaria.service.TransactionImportService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profiles/{profileId}/transaction-imports")
public class TransactionImportController {

  private final TransactionImportService service;
  private final CurrentUserResolver currentUserResolver;

  public TransactionImportController(
          TransactionImportService service,
          CurrentUserResolver currentUserResolver
  ) {
    this.service = service;
    this.currentUserResolver = currentUserResolver;
  }

  @PostMapping("/preview")
  public TransactionImportPreviewResponse preview(
          @RequestHeader(value = "X-User-Id", required = false) String userHeader,
          @PathVariable UUID profileId,
          @RequestParam TransactionImportSource source,
          @RequestParam UUID accountId,
          @RequestParam(required = false) Integer year,
          @RequestParam(required = false) Integer month,
          @RequestPart("file") MultipartFile file
  ) {
    return service.preview(
            currentUserResolver.parse(userHeader),
            profileId,
            accountId,
            source,
            file,
            year,
            month
    );
  }

  @GetMapping("/{batchId}")
  public TransactionImportPreviewResponse getBatch(
          @RequestHeader(value = "X-User-Id", required = false) String userHeader,
          @PathVariable UUID profileId,
          @PathVariable UUID batchId
  ) {
    return service.getBatch(
            currentUserResolver.parse(userHeader),
            profileId,
            batchId
    );
  }

  @PostMapping("/{batchId}/commit")
  public TransactionImportCommitResponse commit(
          @RequestHeader(value = "X-User-Id", required = false) String userHeader,
          @PathVariable UUID profileId,
          @PathVariable UUID batchId,
          @Valid @RequestBody TransactionImportCommitRequest request
  ) {
    return service.commit(
            currentUserResolver.parse(userHeader),
            profileId,
            batchId,
            request
    );
  }
}