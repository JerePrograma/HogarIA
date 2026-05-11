package com.hogaria.controller;

import com.hogaria.dto.ExternalLoanSyncConfigDtos.*;
import com.hogaria.integration.cjprestamos.dto.ExternalIntegrationDiagnosticResponse;
import com.hogaria.integration.cjprestamos.dto.ExternalLoanManualSyncResponse;
import com.hogaria.integration.cjprestamos.dto.ExternalLoansSummaryResponse;
import com.hogaria.security.CurrentUserResolver;
import com.hogaria.service.ExternalLoanSyncConfigService;
import com.hogaria.service.ExternalLoansService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profiles/{profileId}/external-loans")
public class ExternalLoansController {
  private final ExternalLoansService service;
  private final ExternalLoanSyncConfigService syncConfigService;
  private final CurrentUserResolver currentUserResolver;

  public ExternalLoansController(ExternalLoansService service, ExternalLoanSyncConfigService syncConfigService, CurrentUserResolver currentUserResolver) {
    this.service = service;
    this.syncConfigService = syncConfigService;
    this.currentUserResolver = currentUserResolver;
  }

  @GetMapping("/summary")
  public ExternalLoansSummaryResponse summary(@PathVariable UUID profileId, @RequestHeader("X-User-Id") String xUserId) {
    return service.getSummary(currentUserResolver.parse(xUserId), profileId);
  }

  @PostMapping("/sync")
  public ExternalLoanManualSyncResponse sync(@PathVariable UUID profileId, @RequestHeader("X-User-Id") String xUserId) {
    return service.sync(currentUserResolver.parse(xUserId), profileId);
  }

  @GetMapping("/health")
  public ExternalIntegrationDiagnosticResponse health(@PathVariable UUID profileId, @RequestHeader("X-User-Id") String xUserId) {
    return service.checkIntegration(currentUserResolver.parse(xUserId), profileId);
  }

  @PostMapping("/sync/dry-run")
  public ExternalLoanManualSyncResponse dryRunSync(@PathVariable UUID profileId, @RequestHeader("X-User-Id") String xUserId) {
    return service.dryRunSync(currentUserResolver.parse(xUserId), profileId);
  }

  @GetMapping("/sync-config")
  public ExternalLoanSyncConfigResponse getSyncConfig(@PathVariable UUID profileId, @RequestHeader("X-User-Id") String xUserId) {
    return syncConfigService.get(currentUserResolver.parse(xUserId), profileId);
  }

  @PutMapping("/sync-config")
  public ExternalLoanSyncConfigResponse upsertSyncConfig(@PathVariable UUID profileId, @RequestHeader("X-User-Id") String xUserId, @Valid @RequestBody ExternalLoanSyncConfigUpsertRequest request) {
    return syncConfigService.upsert(currentUserResolver.parse(xUserId), profileId, request);
  }
}
