package com.hogaria.controller;

import com.hogaria.dto.MonthlyPlanReconciliationDtos.*;
import com.hogaria.security.CurrentUserResolver;
import com.hogaria.service.MonthlyPlanReconciliationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/profiles/{profileId}/planning/reconciliation")
public class MonthlyPlanReconciliationController {
  private final MonthlyPlanReconciliationService s; private final CurrentUserResolver cu;
  public MonthlyPlanReconciliationController(MonthlyPlanReconciliationService s, CurrentUserResolver cu){this.s=s;this.cu=cu;}
  @GetMapping public MonthlyPlanReconciliationSummary summary(@RequestHeader("X-User-Id") String h,@PathVariable UUID profileId,@RequestParam int year,@RequestParam int month){ return s.getSummary(cu.parse(h),profileId,year,month); }
  @PostMapping("/matches") public TransactionMatch confirm(@RequestHeader("X-User-Id") String h,@PathVariable UUID profileId,@Valid @RequestBody ConfirmPlanTransactionMatchPayload payload){ return s.confirm(cu.parse(h),profileId,payload); }
  @DeleteMapping("/matches/{matchId}") public void delete(@RequestHeader("X-User-Id") String h,@PathVariable UUID profileId,@PathVariable UUID matchId){ s.delete(cu.parse(h),profileId,matchId); }
}
