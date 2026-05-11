package com.hogaria.controller;

import com.hogaria.integration.cjprestamos.dto.ExternalLoansSummaryResponse;
import com.hogaria.security.CurrentUserResolver;
import com.hogaria.service.ExternalLoansService;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profiles/{profileId}/external-loans")
public class ExternalLoansController {
  private final ExternalLoansService service;
  private final CurrentUserResolver currentUserResolver;

  public ExternalLoansController(ExternalLoansService service, CurrentUserResolver currentUserResolver) {
    this.service = service;
    this.currentUserResolver = currentUserResolver;
  }

  @GetMapping("/summary")
  public ExternalLoansSummaryResponse summary(@PathVariable UUID profileId, @RequestHeader("X-User-Id") String xUserId) {
    return service.getSummary(currentUserResolver.parse(xUserId), profileId);
  }
}
