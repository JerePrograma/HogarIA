package com.hogaria.integration.cjprestamos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExternalLoanInstallmentResponse(
    Long externalInstallmentId,
    Long externalLoanId,
    Integer installmentNumber,
    LocalDate dueDate,
    BigDecimal scheduledAmount,
    BigDecimal paidAmount,
    BigDecimal pendingAmount,
    String status
) {}
