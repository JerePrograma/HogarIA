package com.hogaria.controller;

import com.hogaria.dto.QuickPlanTextDtos.*;
import com.hogaria.security.CurrentUserResolver;
import com.hogaria.service.QuickPlanTextImportService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class QuickPlanTextImportController {
  private final QuickPlanTextImportService service;
  private final CurrentUserResolver currentUserResolver;

  public QuickPlanTextImportController(QuickPlanTextImportService service, CurrentUserResolver currentUserResolver) {
    this.service = service;
    this.currentUserResolver = currentUserResolver;
  }

  @PostMapping("/profiles/{profileId}/planning/quick-text/preview")
  public QuickPlanTextPreviewResponse preview(@RequestHeader(value = "X-User-Id", required = false) String h, @PathVariable UUID profileId, @Valid @RequestBody QuickPlanTextPreviewRequest request) {
    return service.preview(currentUserResolver.parse(h), profileId, request);
  }

  @PostMapping("/profiles/{profileId}/planning/quick-text/commit")
  public QuickPlanTextCommitResponse commit(@RequestHeader(value = "X-User-Id", required = false) String h, @PathVariable UUID profileId, @Valid @RequestBody QuickPlanTextCommitRequest request) {
    return service.commit(currentUserResolver.parse(h), profileId, request);
  }
}
