package com.hogaria.integration.cjprestamos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExternalLoanPaymentResponse(
    Long externalPaymentId,
    Long externalLoanId,
    LocalDate paymentDate,
    BigDecimal amount,
    String manualReference,
    String notes,
    String status
) {}
