package com.hogaria.service;

import com.hogaria.entity.Category;
import com.hogaria.entity.MoneyTransaction;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class TransactionFinancialImpactService {

    public TransactionFinancialImpact analyze(MoneyTransaction tx, Category category, String externalEventType) {
        var treatment = classifyTreatment(tx, category, externalEventType);
        var balanceImpact = toBalanceImpact(tx, treatment);

        boolean ignored = tx.getStatus() == MoneyTransaction.Status.IGNORED
                || tx.getClassificationStatus() == MoneyTransaction.ClassificationStatus.IGNORED_BY_RULE
                || tx.getBalanceImpact() == MoneyTransaction.BalanceImpact.IGNORED;
        boolean technical = tx.getClassificationStatus() == MoneyTransaction.ClassificationStatus.TECHNICAL
                || tx.getBalanceImpact() == MoneyTransaction.BalanceImpact.TECHNICAL;

        boolean income = treatment == CashFlowTreatment.EARNED_INCOME
                || treatment == CashFlowTreatment.INTEREST_INCOME;
        boolean consumption = treatment == CashFlowTreatment.CONSUMPTION_EXPENSE
                || treatment == CashFlowTreatment.FIXED_CONSUMPTION_EXPENSE
                || treatment == CashFlowTreatment.VARIABLE_CONSUMPTION_EXPENSE;
        boolean saving = treatment == CashFlowTreatment.SAVING_OUTFLOW;
        boolean investment = treatment == CashFlowTreatment.INVESTMENT_OUTFLOW;
        boolean debt = treatment == CashFlowTreatment.DEBT_OUTFLOW
                || treatment == CashFlowTreatment.DEBT_INTEREST;
        boolean operating = !ignored
                && !technical
                && !isNeutralTreatment(treatment)
                && (income || consumption || saving || investment || debt);

        return new TransactionFinancialImpact(
                treatment,
                balanceImpact,
                operating,
                income,
                consumption,
                saving,
                investment,
                debt,
                treatment == CashFlowTreatment.INTERNAL_TRANSFER,
                treatment == CashFlowTreatment.EXTERNAL_TRANSFER,
                treatment == CashFlowTreatment.NEUTRAL_ADJUSTMENT,
                treatment == CashFlowTreatment.RECOVERABLE_OUTFLOW,
                treatment == CashFlowTreatment.PRINCIPAL_RECOVERY,
                treatment == CashFlowTreatment.REFUND_OR_REIMBURSEMENT,
                treatment == CashFlowTreatment.INTEREST_INCOME,
                ignored,
                technical
        );
    }

    public CashFlowTreatment classifyTreatment(MoneyTransaction tx, Category category, String externalEventType) {
        if (tx.getStatus() == MoneyTransaction.Status.IGNORED
                || tx.getClassificationStatus() == MoneyTransaction.ClassificationStatus.IGNORED_BY_RULE) {
            return CashFlowTreatment.NEUTRAL_ADJUSTMENT;
        }

        if (externalEventType != null) {
            return switch (externalEventType) {
                case "DISBURSEMENT" -> CashFlowTreatment.RECOVERABLE_OUTFLOW;
                case "PAYMENT_PRINCIPAL_RECOVERY" -> CashFlowTreatment.PRINCIPAL_RECOVERY;
                case "PAYMENT_INTEREST_INCOME" -> CashFlowTreatment.INTEREST_INCOME;
                default -> CashFlowTreatment.UNKNOWN;
            };
        }

        var classificationReason = tx.getClassificationReason();
        if (classificationReason != null) {
            switch (classificationReason) {
                case "CJPRESTAMOS_DISBURSEMENT" -> {
                    return CashFlowTreatment.RECOVERABLE_OUTFLOW;
                }
                case "CJPRESTAMOS_PAYMENT_PRINCIPAL_RECOVERY" -> {
                    return CashFlowTreatment.PRINCIPAL_RECOVERY;
                }
                case "CJPRESTAMOS_PAYMENT_INTEREST_INCOME" -> {
                    return CashFlowTreatment.INTEREST_INCOME;
                }
                case "RULE_INTEREST_INCOME" -> {
                    return CashFlowTreatment.INTEREST_INCOME;
                }
                case "RULE_BENEFIT_REIMBURSEMENT" -> {
                    return CashFlowTreatment.REFUND_OR_REIMBURSEMENT;
                }
                case "RULE_LOAN_CAPITAL_RECOVERY" -> {
                    return CashFlowTreatment.PRINCIPAL_RECOVERY;
                }
                case "RULE_MERCADO_CREDITO_DEBT_PAYMENT" -> {
                    return CashFlowTreatment.DEBT_OUTFLOW;
                }
                case "POSSIBLE_INTERNAL_TRANSFER", "INTERNAL_TRANSFER_MATCHED", "USER_MARKED_INTERNAL_TRANSFER",
                        "TRANSFER_UNMATCHED", "RULE_MP_FUNDING_TRANSFER", "RULE_INTERNAL_TRANSFER_TRASPASO" -> {
                    return CashFlowTreatment.INTERNAL_TRANSFER;
                }
                case "POSSIBLE_CROSS_SOURCE_DUPLICATE", "USER_IGNORED_CROSS_SOURCE" -> {
                    return CashFlowTreatment.UNKNOWN;
                }
                default -> {
                }
            }
        }

        if (tx.getInternalTransferGroupId() != null
                || tx.getPaymentChannel() == MoneyTransaction.PaymentChannel.INTERNAL_TRANSFER) {
            return CashFlowTreatment.INTERNAL_TRANSFER;
        }

        var explicitTreatment = treatmentFromBalanceImpact(tx.getBalanceImpact());
        if (explicitTreatment != null) {
            return explicitTreatment;
        }

        if (tx.getClassificationStatus() == MoneyTransaction.ClassificationStatus.TECHNICAL
                && tx.getMovementType() == MoneyTransaction.MovementType.TRANSFER) {
            return CashFlowTreatment.INTERNAL_TRANSFER;
        }

        if (tx.getMovementType() == MoneyTransaction.MovementType.TRANSFER) {
            return isInternalTransferText(tx)
                    ? CashFlowTreatment.INTERNAL_TRANSFER
                    : CashFlowTreatment.EXTERNAL_TRANSFER;
        }

        if (tx.getMovementType() == MoneyTransaction.MovementType.ADJUSTMENT) {
            return isRefundText(tx)
                    ? CashFlowTreatment.REFUND_OR_REIMBURSEMENT
                    : CashFlowTreatment.NEUTRAL_ADJUSTMENT;
        }

        if (tx.getMovementType() == MoneyTransaction.MovementType.INCOME) {
            return CashFlowTreatment.EARNED_INCOME;
        }

        if (category == null) {
            return CashFlowTreatment.UNKNOWN;
        }

        return switch (category.getType()) {
            case FIXED_EXPENSE -> CashFlowTreatment.FIXED_CONSUMPTION_EXPENSE;
            case VARIABLE_EXPENSE -> CashFlowTreatment.VARIABLE_CONSUMPTION_EXPENSE;
            case DEBT -> CashFlowTreatment.DEBT_OUTFLOW;
            case SAVING -> CashFlowTreatment.SAVING_OUTFLOW;
            case INVESTMENT -> CashFlowTreatment.INVESTMENT_OUTFLOW;
            case INCOME -> CashFlowTreatment.EARNED_INCOME;
        };
    }

    private MoneyTransaction.BalanceImpact toBalanceImpact(MoneyTransaction tx, CashFlowTreatment treatment) {
        if (tx.getStatus() == MoneyTransaction.Status.IGNORED
                || tx.getClassificationStatus() == MoneyTransaction.ClassificationStatus.IGNORED_BY_RULE) {
            return MoneyTransaction.BalanceImpact.IGNORED;
        }
        if (tx.getClassificationStatus() == MoneyTransaction.ClassificationStatus.TECHNICAL
                && treatment != CashFlowTreatment.INTERNAL_TRANSFER) {
            return MoneyTransaction.BalanceImpact.TECHNICAL;
        }
        if (tx.getBalanceImpact() == MoneyTransaction.BalanceImpact.IGNORED) {
            return MoneyTransaction.BalanceImpact.IGNORED;
        }
        if (tx.getBalanceImpact() == MoneyTransaction.BalanceImpact.TECHNICAL
                && treatment != CashFlowTreatment.INTERNAL_TRANSFER) {
            return MoneyTransaction.BalanceImpact.TECHNICAL;
        }

        if (treatment != CashFlowTreatment.UNKNOWN) {
            return balanceImpactFromTreatment(treatment);
        }

        if (tx.getBalanceImpact() == MoneyTransaction.BalanceImpact.IGNORED
                || tx.getBalanceImpact() == MoneyTransaction.BalanceImpact.TECHNICAL) {
            return tx.getBalanceImpact();
        }

        return MoneyTransaction.BalanceImpact.UNKNOWN;
    }

    private MoneyTransaction.BalanceImpact balanceImpactFromTreatment(CashFlowTreatment treatment) {
        return switch (treatment) {
            case EARNED_INCOME -> MoneyTransaction.BalanceImpact.OPERATING_INCOME;
            case INTEREST_INCOME -> MoneyTransaction.BalanceImpact.INTEREST_INCOME;
            case FIXED_CONSUMPTION_EXPENSE, VARIABLE_CONSUMPTION_EXPENSE, CONSUMPTION_EXPENSE ->
                    MoneyTransaction.BalanceImpact.CONSUMPTION_EXPENSE;
            case SAVING_OUTFLOW -> MoneyTransaction.BalanceImpact.SAVING_OUTFLOW;
            case INVESTMENT_OUTFLOW -> MoneyTransaction.BalanceImpact.INVESTMENT_OUTFLOW;
            case DEBT_OUTFLOW, DEBT_INTEREST -> MoneyTransaction.BalanceImpact.DEBT_OUTFLOW;
            case RECOVERABLE_OUTFLOW -> MoneyTransaction.BalanceImpact.RECOVERABLE_OUTFLOW;
            case PRINCIPAL_RECOVERY -> MoneyTransaction.BalanceImpact.PRINCIPAL_RECOVERY;
            case REFUND_OR_REIMBURSEMENT -> MoneyTransaction.BalanceImpact.REFUND_OR_REIMBURSEMENT;
            case INTERNAL_TRANSFER -> MoneyTransaction.BalanceImpact.INTERNAL_TRANSFER;
            case EXTERNAL_TRANSFER -> MoneyTransaction.BalanceImpact.EXTERNAL_TRANSFER;
            case NEUTRAL_ADJUSTMENT -> MoneyTransaction.BalanceImpact.NEUTRAL_ADJUSTMENT;
            case UNKNOWN -> MoneyTransaction.BalanceImpact.UNKNOWN;
        };
    }

    private CashFlowTreatment treatmentFromBalanceImpact(MoneyTransaction.BalanceImpact balanceImpact) {
        if (balanceImpact == null || balanceImpact == MoneyTransaction.BalanceImpact.UNKNOWN) {
            return null;
        }

        return switch (balanceImpact) {
            case OPERATING_INCOME -> CashFlowTreatment.EARNED_INCOME;
            case INTEREST_INCOME -> CashFlowTreatment.INTEREST_INCOME;
            case CONSUMPTION_EXPENSE -> CashFlowTreatment.CONSUMPTION_EXPENSE;
            case SAVING_OUTFLOW -> CashFlowTreatment.SAVING_OUTFLOW;
            case INVESTMENT_OUTFLOW -> CashFlowTreatment.INVESTMENT_OUTFLOW;
            case DEBT_OUTFLOW -> CashFlowTreatment.DEBT_OUTFLOW;
            case RECOVERABLE_OUTFLOW -> CashFlowTreatment.RECOVERABLE_OUTFLOW;
            case PRINCIPAL_RECOVERY -> CashFlowTreatment.PRINCIPAL_RECOVERY;
            case REFUND_OR_REIMBURSEMENT -> CashFlowTreatment.REFUND_OR_REIMBURSEMENT;
            case INTERNAL_TRANSFER -> CashFlowTreatment.INTERNAL_TRANSFER;
            case EXTERNAL_TRANSFER -> CashFlowTreatment.EXTERNAL_TRANSFER;
            case NEUTRAL_ADJUSTMENT -> CashFlowTreatment.NEUTRAL_ADJUSTMENT;
            case IGNORED, TECHNICAL, UNKNOWN -> null;
        };
    }

    private boolean isNeutralTreatment(CashFlowTreatment treatment) {
        return treatment == CashFlowTreatment.INTERNAL_TRANSFER
                || treatment == CashFlowTreatment.EXTERNAL_TRANSFER
                || treatment == CashFlowTreatment.NEUTRAL_ADJUSTMENT
                || treatment == CashFlowTreatment.RECOVERABLE_OUTFLOW
                || treatment == CashFlowTreatment.PRINCIPAL_RECOVERY
                || treatment == CashFlowTreatment.REFUND_OR_REIMBURSEMENT
                || treatment == CashFlowTreatment.UNKNOWN;
    }

    private boolean isRefundText(MoneyTransaction tx) {
        var description = text(tx);
        return description.contains("REINTEGRO")
                || description.contains("REFUND")
                || description.contains("DEVOL");
    }

    private boolean isInternalTransferText(MoneyTransaction tx) {
        var description = text(tx);
        return description.contains("INTERNA")
                || description.contains("INTERNAL")
                || description.contains("DEBIN")
                || description.contains("CUENTA DNI")
                || description.contains("FONDEO")
                || description.contains("TRASPASO")
                || description.contains("MERCADO PAGO");
    }

    private String text(MoneyTransaction tx) {
        return ((tx.getNormalizedDescription() == null ? "" : tx.getNormalizedDescription())
                + " "
                + (tx.getDescription() == null ? "" : tx.getDescription()))
                .toUpperCase(Locale.ROOT);
    }
}
