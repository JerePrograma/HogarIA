package com.hogaria.controller;

import com.hogaria.dto.BulkRecategorizeApplyRequest;
import com.hogaria.dto.BulkRecategorizeApplyResponse;
import com.hogaria.dto.BulkRecategorizePreviewRequest;
import com.hogaria.dto.BulkRecategorizePreviewResponse;
import com.hogaria.dto.TransactionCreateRequest;
import com.hogaria.dto.TransactionResponse;
import com.hogaria.dto.TransactionUpdateRequest;
import com.hogaria.security.CurrentUserResolver;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api")
public class TransactionController {

  private final TransactionService service;
  private final CurrentUserResolver parser;

  public TransactionController(
          TransactionService service,
          CurrentUserResolver parser
  ) {
    this.service = service;
    this.parser = parser;
  }

  @PostMapping("/transactions")
  public TransactionResponse create(
          @RequestHeader(value = "X-User-Id", required = false) String userHeader,
          @Valid @RequestBody TransactionCreateRequest request
  ) {
    return service.create(request, parser.parse(userHeader));
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
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
          @RequestHeader(value = "X-User-Id", required = false) String userHeader,
          @PathVariable UUID id
  ) {
    service.delete(parser.parse(userHeader), id);
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
}
