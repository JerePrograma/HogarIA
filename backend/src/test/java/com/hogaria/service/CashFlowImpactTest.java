package com.hogaria.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hogaria.entity.Category;
import com.hogaria.entity.MoneyTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CashFlowImpactTest {

    TransactionFinancialImpactService service = new TransactionFinancialImpactService();

    @Test
    void incomeImpactsIncome() {
        var impact = service.analyze(tx(MoneyTransaction.MovementType.INCOME), category(Category.Type.INCOME), null);

        assertTrue(impact.impactsIncome());
        assertTrue(impact.impactsOperationalBalance());
    }

    @Test
    void expenseImpactsConsumptionExpense() {
        var impact = service.analyze(tx(MoneyTransaction.MovementType.EXPENSE), category(Category.Type.VARIABLE_EXPENSE), null);

        assertTrue(impact.impactsConsumptionExpense());
        assertTrue(impact.impactsOperationalBalance());
    }

    @Test
    void savingAndInvestmentAreSeparatedFromConsumption() {
        var saving = service.analyze(tx(MoneyTransaction.MovementType.SAVING), category(Category.Type.SAVING), null);
        var investment = service.analyze(tx(MoneyTransaction.MovementType.SAVING), category(Category.Type.INVESTMENT), null);

        assertTrue(saving.impactsSaving());
        assertTrue(investment.impactsInvestment());
        assertFalse(saving.impactsConsumptionExpense());
    }

    @Test
    void internalTransferIsNeutral() {
        var tx = tx(MoneyTransaction.MovementType.TRANSFER);
        tx.setInternalTransferGroupId(UUID.randomUUID());
        var impact = service.analyze(tx, null, null);

        assertTrue(impact.internalTransfer());
        assertFalse(impact.impactsIncome());
        assertFalse(impact.impactsConsumptionExpense());
        assertFalse(impact.impactsOperationalBalance());
    }

    @Test
    void explicitInternalTransferBalanceImpactIsNeutralEvenWhenMovementTypeIsWrong() {
        var tx = tx(MoneyTransaction.MovementType.EXPENSE);
        tx.setBalanceImpact(MoneyTransaction.BalanceImpact.INTERNAL_TRANSFER);

        var impact = service.analyze(tx, category(Category.Type.VARIABLE_EXPENSE), null);

        assertTrue(impact.internalTransfer());
        assertFalse(impact.impactsConsumptionExpense());
        assertFalse(impact.impactsOperationalBalance());
        assertEquals(MoneyTransaction.BalanceImpact.INTERNAL_TRANSFER, impact.balanceImpact());
    }

    @Test
    void neutralAdjustmentIgnoredAndTechnicalDoNotImpact() {
        var adjustment = service.analyze(tx(MoneyTransaction.MovementType.ADJUSTMENT), null, null);
        var ignored = tx(MoneyTransaction.MovementType.EXPENSE);
        ignored.setStatus(MoneyTransaction.Status.IGNORED);
        var technical = tx(MoneyTransaction.MovementType.TRANSFER);
        technical.setClassificationStatus(MoneyTransaction.ClassificationStatus.TECHNICAL);

        assertTrue(adjustment.neutralAdjustment());
        assertFalse(adjustment.impactsOperationalBalance());
        assertFalse(service.analyze(ignored, category(Category.Type.VARIABLE_EXPENSE), null).impactsOperationalBalance());
        assertFalse(service.analyze(technical, null, null).impactsOperationalBalance());
    }

    private MoneyTransaction tx(MoneyTransaction.MovementType movementType) {
        return MoneyTransaction.builder()
                .id(UUID.randomUUID())
                .profileId(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .movementType(movementType)
                .realDate(LocalDate.of(2026, 5, 1))
                .budgetDate(LocalDate.of(2026, 5, 1))
                .amount(new BigDecimal("100.00"))
                .currency("ARS")
                .description("Test")
                .status(MoneyTransaction.Status.CONFIRMED)
                .classificationStatus(MoneyTransaction.ClassificationStatus.CLASSIFIED)
                .build();
    }

    private Category category(Category.Type type) {
        return Category.builder()
                .id(UUID.randomUUID())
                .name("Cat")
                .type(type)
                .active(true)
                .build();
    }
}
