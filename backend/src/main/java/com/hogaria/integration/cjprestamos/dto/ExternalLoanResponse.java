package com.hogaria.integration.cjprestamos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ExternalLoanResponse(
    UUID loanId,
    String borrowerName,
    BigDecimal principalAmount,
    BigDecimal outstandingPrincipal,
    BigDecimal annualRate,
    LocalDate startDate,
    LocalDate nextDueDate,
    String status,
    Integer installmentsTotal,
    Integer installmentsPending,
    BigDecimal overdueAmount
) {}
