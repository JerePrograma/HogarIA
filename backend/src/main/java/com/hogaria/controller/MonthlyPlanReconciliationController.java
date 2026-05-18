package com.hogaria.controller;

import com.hogaria.dto.MonthlyPlanReconciliationDtos.ConfirmPlanTransactionMatchRequest;
import com.hogaria.dto.MonthlyPlanReconciliationDtos.MonthlyPlanReconciliationSummaryResponse;
import com.hogaria.dto.MonthlyPlanReconciliationDtos.TransactionMatchResponse;
import com.hogaria.security.CurrentUserResolver;
import com.hogaria.service.MonthlyPlanReconciliationService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MonthlyPlanReconciliationController {

  private final MonthlyPlanReconciliationService service;
  private final CurrentUserResolver currentUserResolver;

  public MonthlyPlanReconciliationController(
      MonthlyPlanReconciliationService service,
      CurrentUserResolver currentUserResolver
  ) {
    this.service = service;
    this.currentUserResolver = currentUserResolver;
  }

  @GetMapping("/profiles/{profileId}/planning/reconciliation")
  public MonthlyPlanReconciliationSummaryResponse monthly(
      @RequestHeader("X-User-Id") String userHeader,
      @PathVariable UUID profileId,
      @RequestParam int year,
      @RequestParam int month
  ) {
    return service.monthly(currentUserResolver.parse(userHeader), profileId, year, month);
  }

  @PostMapping("/profiles/{profileId}/planning/reconciliation/matches")
  public TransactionMatchResponse confirm(
      @RequestHeader("X-User-Id") String userHeader,
      @PathVariable UUID profileId,
      @Valid @RequestBody ConfirmPlanTransactionMatchRequest request
  ) {
    return service.confirm(currentUserResolver.parse(userHeader), profileId, request);
  }

  @DeleteMapping("/profiles/{profileId}/planning/reconciliation/matches/{matchId}")
  public void delete(
      @RequestHeader("X-User-Id") String userHeader,
      @PathVariable UUID profileId,
      @PathVariable UUID matchId
  ) {
    service.delete(currentUserResolver.parse(userHeader), profileId, matchId);
  }
}
