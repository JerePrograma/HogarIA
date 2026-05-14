package com.hogaria.controller;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hogaria.dto.ExternalLoanSyncConfigDtos.ExternalLoanSyncConfigResponse;
import com.hogaria.dto.ExternalLoanSyncConfigDtos.ExternalLoanSyncConfigUpsertRequest;
import com.hogaria.integration.cjprestamos.dto.ExternalIntegrationDiagnosticResponse;
import com.hogaria.integration.cjprestamos.dto.ExternalLoanManualSyncResponse;
import com.hogaria.integration.cjprestamos.dto.ExternalLoansSummaryResponse;
import com.hogaria.security.CurrentUserResolver;
import com.hogaria.service.ExternalLoanSyncConfigService;
import com.hogaria.service.ExternalLoansService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExternalLoansControllerTest {
  @Mock ExternalLoansService service;
  @Mock ExternalLoanSyncConfigService syncConfigService;
  @Mock CurrentUserResolver currentUserResolver;

  private ExternalLoansController controller;

  @BeforeEach
  void setUp() {
    controller = new ExternalLoansController(service, syncConfigService, currentUserResolver);
  }

  @Test
  void summaryDelegatesToService() {
    UUID profileId = UUID.randomUUID(); UUID userId = UUID.randomUUID();
    String header = userId.toString();
    var response = ExternalLoansSummaryResponse.disabled();
    when(currentUserResolver.parse(header)).thenReturn(userId);
    when(service.getSummary(userId, profileId)).thenReturn(response);

    assertSame(response, controller.summary(profileId, header));
  }

  @Test
  void healthDelegatesToService() {
    UUID profileId = UUID.randomUUID(); UUID userId = UUID.randomUUID();
    String header = userId.toString();
    var response = ExternalIntegrationDiagnosticResponse.of("OK", "ok", true, false, "http://x", "/api", true, true, 1000, 1000, true, List.of());
    when(currentUserResolver.parse(header)).thenReturn(userId);
    when(service.checkIntegration(userId, profileId)).thenReturn(response);

    assertSame(response, controller.health(profileId, header));
  }

  @Test
  void dryRunDelegatesToService() {
    UUID profileId = UUID.randomUUID(); UUID userId = UUID.randomUUID();
    String header = userId.toString();
    var response = new ExternalLoanManualSyncResponse(true, 0, 0, 0, 0, List.of(), List.of(), List.of(), List.of(), java.util.Map.of());
    when(currentUserResolver.parse(header)).thenReturn(userId);
    when(service.dryRunSync(userId, profileId)).thenReturn(response);

    assertSame(response, controller.dryRunSync(profileId, header));
  }

  @Test
  void syncBlockedDelegatesErrorFromService() {
    UUID profileId = UUID.randomUUID(); UUID userId = UUID.randomUUID();
    String header = userId.toString();
    when(currentUserResolver.parse(header)).thenReturn(userId);

    controller.sync(profileId, header);
    verify(service).sync(userId, profileId);
  }

  @Test
  void getAndPutSyncConfigDelegateToService() {
    UUID profileId = UUID.randomUUID(); UUID userId = UUID.randomUUID();
    String header = userId.toString();
    var request = new ExternalLoanSyncConfigUpsertRequest(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), true);
    var response = new ExternalLoanSyncConfigResponse(UUID.randomUUID(), profileId, request.accountId(), request.loanDisbursementCategoryId(), request.principalRecoveryCategoryId(), request.interestIncomeCategoryId(), true, null, null);
    when(currentUserResolver.parse(header)).thenReturn(userId);
    when(syncConfigService.get(userId, profileId)).thenReturn(response);
    when(syncConfigService.upsert(userId, profileId, request)).thenReturn(response);

    assertSame(response, controller.getSyncConfig(profileId, header));
    assertSame(response, controller.upsertSyncConfig(profileId, header, request));
  }
}
