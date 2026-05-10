package com.hogaria.controller;

import com.hogaria.dto.QuickCaptureDtos.*;
import com.hogaria.security.CurrentUserResolver;
import com.hogaria.service.MonthlyPlanQuickCaptureService;
import com.hogaria.service.MonthlyPlanService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class MonthlyPlanQuickCaptureController {
  private final MonthlyPlanQuickCaptureService quickCaptureService;
  private final MonthlyPlanService monthlyPlanService;
  private final CurrentUserResolver currentUserResolver;

  public MonthlyPlanQuickCaptureController(MonthlyPlanQuickCaptureService quickCaptureService, MonthlyPlanService monthlyPlanService, CurrentUserResolver currentUserResolver) {
    this.quickCaptureService = quickCaptureService;
    this.monthlyPlanService = monthlyPlanService;
    this.currentUserResolver = currentUserResolver;
  }

  @PostMapping("/profiles/{profileId}/planning/quick-capture/preview")
  public QuickCapturePreviewResponse preview(@RequestHeader("X-User-Id") String h, @PathVariable UUID profileId, @Valid @RequestBody QuickCapturePreviewRequest request) {
    return quickCaptureService.preview(currentUserResolver.parse(h), profileId, request);
  }

  @PostMapping("/profiles/{profileId}/planning/quick-capture/commit")
  public QuickCaptureCommitResponse commit(@RequestHeader("X-User-Id") String h, @PathVariable UUID profileId, @Valid @RequestBody QuickCaptureCommitRequest request) {
    return new QuickCaptureCommitResponse(monthlyPlanService.create(currentUserResolver.parse(h), profileId, request.payload()), List.of());
  }
}
