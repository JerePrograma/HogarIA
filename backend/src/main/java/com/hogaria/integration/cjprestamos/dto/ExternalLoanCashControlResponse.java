package com.hogaria.integration.cjprestamos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExternalLoanCashControlResponse(
    LocalDate date,
    BigDecimal openingBalance,
    BigDecimal disbursedAmount,
    BigDecimal collectedPrincipal,
    BigDecimal collectedInterest,
    BigDecimal collectedLateFees,
    BigDecimal closingBalance
) {}
