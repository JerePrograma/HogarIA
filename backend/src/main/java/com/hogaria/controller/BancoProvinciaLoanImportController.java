package com.hogaria.controller;

import com.hogaria.dto.BancoProvinciaLoanImportDtos.BancoProvinciaLoanCommitRequest;
import com.hogaria.dto.BancoProvinciaLoanImportDtos.BancoProvinciaLoanCommitResponse;
import com.hogaria.dto.BancoProvinciaLoanImportDtos.BancoProvinciaLoanPreviewResponse;
import com.hogaria.security.CurrentUserResolver;
import com.hogaria.service.BancoProvinciaLoanImportService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profiles/{profileId}/external-debts/banco-provincia")
public class BancoProvinciaLoanImportController {
  private final BancoProvinciaLoanImportService service; private final CurrentUserResolver currentUserResolver;
  public BancoProvinciaLoanImportController(BancoProvinciaLoanImportService service, CurrentUserResolver currentUserResolver) { this.service = service; this.currentUserResolver = currentUserResolver; }

  @PostMapping("/preview")
  public BancoProvinciaLoanPreviewResponse preview(@RequestHeader(value = "X-User-Id", required = false) String userHeader, @PathVariable UUID profileId, @RequestParam Integer periodYear, @RequestParam Integer periodMonth, @RequestParam(defaultValue = "ARS") String currency, @RequestParam(defaultValue = "false") boolean createMonthlyCommitments, @RequestPart("file") MultipartFile file) {
    return service.preview(currentUserResolver.parse(userHeader), profileId, periodYear, periodMonth, currency, file);
  }

  @PostMapping("/commit")
  public BancoProvinciaLoanCommitResponse commit(@RequestHeader(value = "X-User-Id", required = false) String userHeader, @PathVariable UUID profileId, @Valid @RequestBody BancoProvinciaLoanCommitRequest request) {
    return service.commit(currentUserResolver.parse(userHeader), profileId, request);
  }
}
