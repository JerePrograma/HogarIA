package com.hogaria.controller;

import com.hogaria.dto.BulkRecategorizeApplyRequest;
import com.hogaria.dto.BulkRecategorizeApplyResponse;
import com.hogaria.dto.BulkRecategorizePreviewRequest;
import com.hogaria.dto.BulkRecategorizePreviewResponse;
import com.hogaria.dto.TransactionBulkDtos.BulkActionResponse;
import com.hogaria.dto.TransactionBulkDtos.BulkCategorizeRequest;
import com.hogaria.dto.TransactionBulkDtos.BulkIgnoreRequest;
import com.hogaria.dto.TransactionBulkDtos.BulkStatusRequest;
import com.hogaria.dto.TransactionCreateRequest;
import com.hogaria.dto.TransactionCreatePreviewDtos.TransactionCreatePreviewResponse;
import com.hogaria.dto.TransactionDeletionResponse;
import com.hogaria.dto.TransactionReviewDtos.DuplicatePreviewRequest;
import com.hogaria.dto.TransactionReviewDtos.DuplicatePreviewResponse;
import com.hogaria.dto.TransactionReviewDtos.DuplicateResolveRequest;
import com.hogaria.dto.TransactionReviewDtos.DuplicateResolveResponse;
import com.hogaria.dto.TransactionReviewDtos.InternalTransferLinkRequest;
import com.hogaria.dto.TransactionReviewDtos.InternalTransferLinkResponse;
import com.hogaria.dto.TransactionReviewDtos.InternalTransferPreviewRequest;
import com.hogaria.dto.TransactionReviewDtos.InternalTransferPreviewResponse;
import com.hogaria.dto.TransactionReviewDtos.InternalTransferUnlinkRequest;
import com.hogaria.dto.TransactionReviewDtos.InternalTransferUnlinkResponse;
import com.hogaria.dto.TransactionResponse;
import com.hogaria.dto.TransactionUpdateRequest;
import com.hogaria.security.CurrentUserResolver;
import com.hogaria.service.InternalTransferMatcherService;
import com.hogaria.service.InternalTransferResolutionService;
import com.hogaria.service.TransactionDuplicateReviewService;
import com.hogaria.service.TransactionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TransactionController {

  private final TransactionService service;
  private final TransactionDuplicateReviewService duplicateReviewService;
  private final InternalTransferMatcherService internalTransferMatcherService;
  private final InternalTransferResolutionService internalTransferResolutionService;
  private final CurrentUserResolver parser;

  public TransactionController(
          TransactionService service,
          TransactionDuplicateReviewService duplicateReviewService,
          InternalTransferMatcherService internalTransferMatcherService,
          InternalTransferResolutionService internalTransferResolutionService,
          CurrentUserResolver parser
  ) {
    this.service = service;
    this.duplicateReviewService = duplicateReviewService;
    this.internalTransferMatcherService = internalTransferMatcherService;
    this.internalTransferResolutionService = internalTransferResolutionService;
    this.parser = parser;
  }

  @PostMapping("/transactions")
  public TransactionResponse create(
          @RequestHeader(value = "X-User-Id", required = false) String userHeader,
          @Valid @RequestBody TransactionCreateRequest request
  ) {
    return service.create(request, parser.parse(userHeader));
  }

  @PostMapping("/profiles/{profileId}/transactions/preview-create")
  public TransactionCreatePreviewResponse previewCreate(
          @RequestHeader(value = "X-User-Id", required = false) String userHeader,
          @PathVariable UUID profileId,
          @Valid @RequestBody TransactionCreateRequest request
  ) {
    return service.previewCreate(parser.parse(userHeader), profileId, request);
  }

  @GetMapping("/profiles/{profileId}/transactions")
  public List<TransactionResponse> list(
          @RequestHeader(value = "X-User-Id", required = false) String userHeader,
          @PathVariable UUID profileId,
          @RequestParam int year,
          @RequestParam int month
  ) {
    return service.list(parser.parse(userHeader), profileId, year, month);
  }

  @GetMapping("/transactions/{id}")
  public TransactionResponse get(
          @RequestHeader(value = "X-User-Id", required = false) String userHeader,
          @PathVariable UUID id
  ) {
    return service.get(parser.parse(userHeader), id);
  }

  @PutMapping("/transactions/{id}")
  public TransactionResponse update(
          @RequestHeader(value = "X-User-Id", required = false) String userHeader,
          @PathVariable UUID id,
          @Valid @RequestBody TransactionUpdateRequest request
  ) {
    return service.update(parser.parse(userHeader), id, request);
  }

  @DeleteMapping("/transactions/{id}")
  public TransactionDeletionResponse delete(
          @RequestHeader(value = "X-User-Id", required = false) String userHeader,
          @PathVariable UUID id
  ) {
    return service.delete(parser.parse(userHeader), id);
  }

  @PostMapping("/profiles/{profileId}/transactions/bulk-recategorize/preview")
  public BulkRecategorizePreviewResponse previewBulkRecategorize(
          @RequestHeader(value = "X-User-Id", required = false) String userHeader,
          @PathVariable UUID profileId,
          @Valid @RequestBody BulkRecategorizePreviewRequest request
  ) {
    return service.previewBulkRecategorize(
            parser.parse(userHeader),
            profileId,
            request
    );
  }

  @PostMapping("/profiles/{profileId}/transactions/bulk-recategorize/apply")
  public BulkRecategorizeApplyResponse applyBulkRecategorize(
          @RequestHeader(value = "X-User-Id", required = false) String userHeader,
          @PathVariable UUID profileId,
          @Valid @RequestBody BulkRecategorizeApplyRequest request
  ) {
    return service.applyBulkRecategorize(
            parser.parse(userHeader),
            profileId,
            request
    );
  }

  @PostMapping("/profiles/{profileId}/transactions/bulk-categorize")
  public BulkActionResponse bulkCategorize(
          @RequestHeader(value = "X-User-Id", required = false) String userHeader,
          @PathVariable UUID profileId,
          @Valid @RequestBody BulkCategorizeRequest request
  ) {
    return service.bulkCategorize(parser.parse(userHeader), profileId, request);
  }

  @PostMapping("/profiles/{profileId}/transactions/bulk-status")
  public BulkActionResponse bulkStatus(
          @RequestHeader(value = "X-User-Id", required = false) String userHeader,
          @PathVariable UUID profileId,
          @Valid @RequestBody BulkStatusRequest request
  ) {
    return service.bulkStatus(parser.parse(userHeader), profileId, request);
  }

  @PostMapping("/profiles/{profileId}/transactions/bulk-ignore")
  public BulkActionResponse bulkIgnore(
          @RequestHeader(value = "X-User-Id", required = false) String userHeader,
          @PathVariable UUID profileId,
          @Valid @RequestBody BulkIgnoreRequest request
  ) {
    return service.bulkIgnore(parser.parse(userHeader), profileId, request);
  }

  @PostMapping("/profiles/{profileId}/transactions/duplicates/preview")
  public DuplicatePreviewResponse previewDuplicates(
          @RequestHeader(value = "X-User-Id", required = false) String userHeader,
          @PathVariable UUID profileId,
          @RequestBody(required = false) DuplicatePreviewRequest request
  ) {
    return duplicateReviewService.preview(
            parser.parse(userHeader),
            profileId,
            request == null ? new DuplicatePreviewRequest(null, null) : request
    );
  }

  @GetMapping("/profiles/{profileId}/transactions/duplicates/preview")
  public DuplicatePreviewResponse previewDuplicatesGet(
          @RequestHeader(value = "X-User-Id", required = false) String userHeader,
          @PathVariable UUID profileId,
          @RequestParam(required = false) Integer year,
          @RequestParam(required = false) Integer month
  ) {
    return duplicateReviewService.preview(
            parser.parse(userHeader),
            profileId,
            new DuplicatePreviewRequest(year, month)
    );
  }

  @PostMapping("/profiles/{profileId}/transactions/duplicates/resolve")
  public DuplicateResolveResponse resolveDuplicates(
          @RequestHeader(value = "X-User-Id", required = false) String userHeader,
          @PathVariable UUID profileId,
          @Valid @RequestBody DuplicateResolveRequest request
  ) {
    return duplicateReviewService.resolve(parser.parse(userHeader), profileId, request);
  }

  @PostMapping("/profiles/{profileId}/transactions/internal-transfers/preview")
  public InternalTransferPreviewResponse previewInternalTransfers(
          @RequestHeader(value = "X-User-Id", required = false) String userHeader,
          @PathVariable UUID profileId,
          @RequestBody(required = false) InternalTransferPreviewRequest request
  ) {
    return internalTransferMatcherService.preview(
            parser.parse(userHeader),
            profileId,
            request == null ? new InternalTransferPreviewRequest(null, null, null) : request
    );
  }

  @GetMapping("/profiles/{profileId}/transactions/internal-transfers/preview")
  public InternalTransferPreviewResponse previewInternalTransfersGet(
          @RequestHeader(value = "X-User-Id", required = false) String userHeader,
          @PathVariable UUID profileId,
          @RequestParam(required = false) Integer year,
          @RequestParam(required = false) Integer month
  ) {
    return internalTransferMatcherService.preview(
            parser.parse(userHeader),
            profileId,
            new InternalTransferPreviewRequest(year, month, null)
    );
  }

  @PostMapping("/profiles/{profileId}/transactions/internal-transfers/link")
  public InternalTransferLinkResponse linkInternalTransfer(
          @RequestHeader(value = "X-User-Id", required = false) String userHeader,
          @PathVariable UUID profileId,
          @Valid @RequestBody InternalTransferLinkRequest request
  ) {
    return internalTransferResolutionService.link(parser.parse(userHeader), profileId, request);
  }

  @PostMapping("/profiles/{profileId}/transactions/internal-transfers/unlink")
  public InternalTransferUnlinkResponse unlinkInternalTransfer(
          @RequestHeader(value = "X-User-Id", required = false) String userHeader,
          @PathVariable UUID profileId,
          @Valid @RequestBody InternalTransferUnlinkRequest request
  ) {
    return internalTransferResolutionService.unlink(parser.parse(userHeader), profileId, request);
  }
}
