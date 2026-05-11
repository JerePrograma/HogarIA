package com.hogaria.integration.cjprestamos.dto;

import java.math.BigDecimal;

public record ExternalLoanCashControlResponse(
    BigDecimal availableCash,
    BigDecimal activeInvestment,
    BigDecimal recoveredCapital,
    BigDecimal pendingCapital,
    BigDecimal realizedProfit,
    BigDecimal projectedProfit,
    BigDecimal currentMonthIncome,
    BigDecimal currentMonthExpense,
    BigDecimal currentMonthBalance,
    BigDecimal projectedCollection30Days,
    BigDecimal projectedCollection60Days,
    BigDecimal projectedCollection90Days,
    BigDecimal overduePortfolio,
    Long pendingInstallments,
    Long dueNext7DaysInstallments,
    BigDecimal capitalRecoveryPercentage,
    BigDecimal expectedYieldPercentage
) {}
