package com.hogaria.integration.cjprestamos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ExternalLoanInstallmentResponse(
    UUID installmentId,
    UUID loanId,
    Integer installmentNumber,
    LocalDate dueDate,
    BigDecimal principalDue,
    BigDecimal interestDue,
    BigDecimal totalDue,
    BigDecimal paidAmount,
    BigDecimal lateFee,
    String status
) {}
