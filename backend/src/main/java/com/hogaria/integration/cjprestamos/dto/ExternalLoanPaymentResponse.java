package com.hogaria.integration.cjprestamos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ExternalLoanPaymentResponse(
    UUID paymentId,
    UUID loanId,
    UUID installmentId,
    LocalDate paymentDate,
    BigDecimal principalPaid,
    BigDecimal interestPaid,
    BigDecimal lateFeePaid,
    BigDecimal totalPaid,
    String paymentMethod,
    String notes
) {}
