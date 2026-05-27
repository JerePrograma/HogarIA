package com.hogaria.service;

import com.hogaria.entity.MoneyTransaction;

public record TransactionFinancialImpact(
        CashFlowTreatment treatment,
        MoneyTransaction.BalanceImpact balanceImpact,
        boolean impactsOperationalBalance,
        boolean impactsIncome,
        boolean impactsConsumptionExpense,
        boolean impactsSaving,
        boolean impactsInvestment,
        boolean impactsDebt,
        boolean internalTransfer,
        boolean externalTransfer,
        boolean neutralAdjustment,
        boolean recoverableOutflow,
        boolean principalRecovery,
        boolean refundOrReimbursement,
        boolean interestIncome,
        boolean ignored,
        boolean technical
) {
}
