package com.hogaria.integration.cjprestamos.dto;

import java.math.BigDecimal;

public record ExternalLoanSummaryResponse(
    BigDecimal investedAmount,
    BigDecimal earnedAmount,
    BigDecimal amountToEarn,
    BigDecimal totalDebt,
    Long activeLoans
) {}
