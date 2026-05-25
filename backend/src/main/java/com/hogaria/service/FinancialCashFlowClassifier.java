package com.hogaria.service;

import com.hogaria.entity.Category;
import com.hogaria.entity.MoneyTransaction;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class FinancialCashFlowClassifier {

  public CashFlowTreatment classify(MoneyTransaction tx, Category category, String externalEventType) {
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
        case "POSSIBLE_INTERNAL_TRANSFER", "INTERNAL_TRANSFER_MATCHED", "USER_MARKED_INTERNAL_TRANSFER" -> {
          return CashFlowTreatment.INTERNAL_TRANSFER;
        }
        case "POSSIBLE_CROSS_SOURCE_DUPLICATE", "USER_IGNORED_CROSS_SOURCE" -> {
          return CashFlowTreatment.UNKNOWN;
        }
        default -> {
        }
      }
    }

    if (tx.getMovementType() == MoneyTransaction.MovementType.TRANSFER) {
      var description = tx.getDescription() == null ? "" : tx.getDescription().toLowerCase(Locale.ROOT);
      return description.contains("interna") || description.contains("internal")
          ? CashFlowTreatment.INTERNAL_TRANSFER
          : CashFlowTreatment.EXTERNAL_TRANSFER;
    }

    if (tx.getMovementType() == MoneyTransaction.MovementType.ADJUSTMENT) {
      var description = tx.getDescription() == null ? "" : tx.getDescription().toLowerCase(Locale.ROOT);
      if (description.contains("reintegro") || description.contains("refund") || description.contains("devol")) {
        return CashFlowTreatment.REFUND_OR_REIMBURSEMENT;
      }
      return CashFlowTreatment.NEUTRAL_ADJUSTMENT;
    }

    if (tx.getMovementType() == MoneyTransaction.MovementType.INCOME) return CashFlowTreatment.EARNED_INCOME;
    if (category == null) return CashFlowTreatment.UNKNOWN;

    return switch (category.getType()) {
      case FIXED_EXPENSE -> CashFlowTreatment.FIXED_CONSUMPTION_EXPENSE;
      case VARIABLE_EXPENSE -> CashFlowTreatment.VARIABLE_CONSUMPTION_EXPENSE;
      case DEBT -> CashFlowTreatment.DEBT_OUTFLOW;
      case SAVING -> CashFlowTreatment.SAVING_OUTFLOW;
      case INVESTMENT -> CashFlowTreatment.INVESTMENT_OUTFLOW;
      case INCOME -> CashFlowTreatment.EARNED_INCOME;
    };
  }
}
