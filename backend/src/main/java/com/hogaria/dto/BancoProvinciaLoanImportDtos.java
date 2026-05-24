package com.hogaria.dto;

import com.hogaria.entity.MonthlyPlanItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class BancoProvinciaLoanImportDtos {
  public record BancoProvinciaLoanPreviewResponse(List<BancoProvinciaLoanCandidate> candidates, List<String> warnings) {}

  public record BancoProvinciaLoanCandidate(
      Integer lineNumber,
      String tipo,
      String identificacion,
      String accountNumber,
      BigDecimal currentDebtAmount,
      BigDecimal originalAmount,
      LocalDate dueDate,
      Integer monthsRemaining,
      BigDecimal estimatedMonthlyAmount,
      String suggestedTitle,
      MonthlyPlanItem.Type suggestedType,
      MonthlyPlanItem.Priority suggestedPriority,
      MonthlyPlanItem.Status suggestedStatus,
      Boolean duplicate,
      List<String> warnings,
      String currency,
      String suggestedCategoryId
  ) {}

  public record BancoProvinciaLoanCommitRequest(
      @NotNull @Min(2000) @Max(2100) Integer periodYear,
      @NotNull @Min(1) @Max(12) Integer periodMonth,
      @NotEmpty List<@Valid BancoProvinciaLoanCommitCandidate> candidates,
      boolean skipDuplicates,
      boolean createMonthlyPlanItems
  ) {}

  public record BancoProvinciaLoanCommitCandidate(
      Integer lineNumber,
      String tipo,
      String identificacion,
      String accountNumber,
      BigDecimal currentDebtAmount,
      BigDecimal originalAmount,
      LocalDate dueDate,
      Integer monthsRemaining,
      BigDecimal estimatedMonthlyAmount,
      String suggestedTitle,
      String currency,
      List<String> warnings
  ) {}

  public record BancoProvinciaLoanCommitResponse(List<MonthlyPlanDtos.MonthlyPlanItemResponse> created, List<String> warnings, Integer skipped) {}
}
