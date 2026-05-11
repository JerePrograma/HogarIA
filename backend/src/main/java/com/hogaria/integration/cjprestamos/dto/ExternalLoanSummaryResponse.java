package com.hogaria.integration.cjprestamos.dto;

import java.math.BigDecimal;

public record ExternalLoanSummaryResponse(
    Integer activeLoans,
    BigDecimal portfolioOutstanding,
    BigDecimal overdueAmount,
    BigDecimal realizedProfit,
    BigDecimal projectedProfit,
    BigDecimal recoveryRate,
    BigDecimal delinquencyRate
) {}
