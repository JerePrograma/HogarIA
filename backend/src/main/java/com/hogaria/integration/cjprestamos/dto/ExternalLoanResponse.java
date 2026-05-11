package com.hogaria.integration.cjprestamos.dto;

import java.math.BigDecimal;

public record ExternalLoanResponse(
    Long externalLoanId,
    Long externalBorrowerId,
    String borrowerName,
    BigDecimal principalAmount,
    BigDecimal totalCollected,
    BigDecimal totalPending,
    BigDecimal realizedProfit,
    BigDecimal projectedProfit,
    String status
) {}
