package com.hogaria.service.transactionimport;

import com.hogaria.dto.TransactionImportDtos.Confidence;
import com.hogaria.dto.TransactionImportDtos.RowStatus;
import com.hogaria.entity.MoneyTransaction;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class TransactionImportRuleClassifier {

  private final ImportTextNormalizer normalizer;

  public TransactionImportRuleClassifier(ImportTextNormalizer normalizer) {
    this.normalizer = normalizer;
  }

  public ImportClassificationResult classifyMercadoPago(
          BigDecimal signedAmount,
          String rawDescription,
          String operationType,
          String paymentMethodType,
          String paymentMethod,
          String payer,
          String liquidated,
          MoneyTransaction.PaymentChannel paymentChannel
  ) {
    var text = normalizedJoin(
            rawDescription,
            operationType,
            paymentMethodType,
            paymentMethod,
            payer,
            liquidated
    );

    var defaultMovement = signedAmount.signum() < 0
            ? MoneyTransaction.MovementType.EXPENSE
            : MoneyTransaction.MovementType.INCOME;

    if (isSmallMercadoPagoTechnicalMovement(signedAmount, rawDescription, paymentMethodType, paymentMethod, payer, liquidated)) {
      return result(
              defaultMovement,
              MoneyTransaction.BalanceImpact.TECHNICAL,
              paymentChannel,
              "diferenciaporredondeo",
              "Diferencia por redondeo",
              MoneyTransaction.ClassificationStatus.TECHNICAL,
              "MP_SMALL_UNLIQUIDATED_NO_DETAIL",
              Confidence.LOW,
              RowStatus.SKIPPED,
              "Movimiento mínimo/no liquidado sin detalle suficiente. Se trata como técnico para no contaminar gastos reales."
      );
    }

    if (contains(text, "pago de creditos de mercado pago")
            || contains(text, "pago de creditos mercado pago")) {
      return result(
              MoneyTransaction.MovementType.EXPENSE,
              MoneyTransaction.BalanceImpact.DEBT_OUTFLOW,
              MoneyTransaction.PaymentChannel.MERCADO_CREDITO,
              "mercadocredito",
              "Mercado Crédito",
              MoneyTransaction.ClassificationStatus.CLASSIFIED,
              "RULE_MERCADO_CREDITO_DEBT_PAYMENT",
              Confidence.HIGH,
              RowStatus.READY,
              null
      );
    }

    if (contains(text, "payment linked to a loan origination")
            || contains(text, "mercadocredito")
            || contains(text, "mercado credito")) {
      return result(
              MoneyTransaction.MovementType.ADJUSTMENT,
              MoneyTransaction.BalanceImpact.NEUTRAL_ADJUSTMENT,
              MoneyTransaction.PaymentChannel.MERCADO_CREDITO,
              "mercadocredito",
              "Mercado Crédito",
              MoneyTransaction.ClassificationStatus.REVIEW,
              "RULE_MERCADO_CREDITO_LOAN_ORIGINATION_REVIEW",
              Confidence.MEDIUM,
              RowStatus.REVIEW,
              "Entrada vinculada a originación de préstamo. Requiere revisar si representa deuda, transferencia técnica o recupero."
      );
    }

    if (contains(text, "uber")) {
      return expense(
              paymentChannel,
              "taxiyapps",
              "Taxi y apps",
              "RULE_UBER",
              Confidence.HIGH
      );
    }

    if (contains(text, "bank transfer")
            || contains(text, "transferencia bancaria")
            || contains(text, "cuenta bancaria digital")
            || contains(text, "debito inmediato")
            || contains(text, "pago debin")) {
      return result(
              MoneyTransaction.MovementType.TRANSFER,
              MoneyTransaction.BalanceImpact.INTERNAL_TRANSFER,
              paymentChannel,
              "fondeomercadopagotransferenciasinternas",
              "Fondeo MercadoPago / transferencias internas",
              MoneyTransaction.ClassificationStatus.TECHNICAL,
              "RULE_MP_FUNDING_TRANSFER",
              Confidence.MEDIUM,
              RowStatus.REVIEW,
              "Movimiento de fondeo/transferencia. Revisar contraparte antes de contarlo como ingreso o gasto real."
      );
    }

    if (contains(text, "link de pago")
            && (contains(text, "prestamos") || contains(text, "prestamo") || contains(text, "moneda digital"))) {
      return result(
              MoneyTransaction.MovementType.SAVING,
              MoneyTransaction.BalanceImpact.PRINCIPAL_RECOVERY,
              paymentChannel,
              "cjcapitalrecuperado",
              "CJ - Capital recuperado",
              MoneyTransaction.ClassificationStatus.CLASSIFIED,
              "RULE_LOAN_CAPITAL_RECOVERY",
              Confidence.HIGH,
              RowStatus.READY,
              "Recupero de capital identificado. No se trata como ingreso operativo."
      );
    }

    if (signedAmount.signum() > 0 && contains(text, "varios")) {
      return result(
              MoneyTransaction.MovementType.INCOME,
              MoneyTransaction.BalanceImpact.UNKNOWN,
              paymentChannel,
              null,
              null,
              MoneyTransaction.ClassificationStatus.REVIEW,
              "RULE_MP_GENERIC_INFLOW_REVIEW",
              Confidence.LOW,
              RowStatus.REVIEW,
              "Ingreso genérico en Mercado Pago. Revisar si es venta, transferencia o movimiento entre cuentas."
      );
    }

    return result(
            defaultMovement,
            defaultMovement == MoneyTransaction.MovementType.EXPENSE
                    ? MoneyTransaction.BalanceImpact.UNKNOWN
                    : MoneyTransaction.BalanceImpact.UNKNOWN,
            paymentChannel,
            null,
            null,
            MoneyTransaction.ClassificationStatus.NEEDS_CATEGORY,
            "NO_IMPORT_RULE",
            Confidence.LOW,
            RowStatus.NEEDS_CATEGORY,
            "No hay regla confiable para categorizar este movimiento."
    );
  }

  public ImportClassificationResult classifyBancoProvincia(
          BigDecimal signedAmount,
          String description,
          String extendedDescription,
          String merchantName,
          MoneyTransaction.PaymentChannel paymentChannel,
          String counterparty
  ) {
    var text = normalizedJoin(description, extendedDescription, merchantName, counterparty);
    var defaultMovement = signedAmount.signum() < 0
            ? MoneyTransaction.MovementType.EXPENSE
            : MoneyTransaction.MovementType.INCOME;

    if (contains(text, "credito haberes") && contains(text, "policia")) {
      return result(
              MoneyTransaction.MovementType.INCOME,
              MoneyTransaction.BalanceImpact.OPERATING_INCOME,
              paymentChannel,
              "sueldo",
              "Sueldo",
              MoneyTransaction.ClassificationStatus.CLASSIFIED,
              "RULE_SALARY_POLICIA",
              Confidence.HIGH,
              RowStatus.READY,
              null
      );
    }

    if (contains(text, "acreditacion intereses") || contains(text, "acredit intereses")) {
      return result(
              MoneyTransaction.MovementType.INCOME,
              MoneyTransaction.BalanceImpact.INTEREST_INCOME,
              paymentChannel,
              "interesesganados",
              "Intereses ganados",
              MoneyTransaction.ClassificationStatus.CLASSIFIED,
              "RULE_INTEREST_INCOME",
              Confidence.HIGH,
              RowStatus.READY,
              null
      );
    }

    if (contains(text, "benef pei cuenta dni")
            || contains(text, "beneficio")
            || contains(text, "benef ")) {
      return result(
              MoneyTransaction.MovementType.INCOME,
              MoneyTransaction.BalanceImpact.REFUND_OR_REIMBURSEMENT,
              paymentChannel,
              "beneficiosypromociones",
              "Beneficios y promociones",
              MoneyTransaction.ClassificationStatus.CLASSIFIED,
              "RULE_BENEFIT_REIMBURSEMENT",
              Confidence.HIGH,
              RowStatus.READY,
              null
      );
    }

    if (contains(text, "payu ar uber") || contains(text, "payu*ar*uber") || contains(text, "uber")) {
      return expense(MoneyTransaction.PaymentChannel.DEBIT_CARD, "taxiyapps", "Taxi y apps", "RULE_UBER", Confidence.HIGH);
    }

    if (contains(text, "busplus")) {
      return expense(
              MoneyTransaction.PaymentChannel.DEBIT_CARD,
              "transportepublico",
              "Transporte público",
              "RULE_BUSPLUS_TRANSPORTE_PUBLICO",
              Confidence.HIGH
      );
    }

    if (contains(text, "sube")) {
      return expense(
              MoneyTransaction.PaymentChannel.DEBIT_CARD,
              "transportepublico",
              "Transporte público",
              "RULE_SUBE_TRANSPORTE_PUBLICO",
              Confidence.HIGH
      );
    }

    if (contains(text, "superdia") || contains(text, "minimercado") || contains(text, "supermercado")) {
      return expense(
              MoneyTransaction.PaymentChannel.DEBIT_CARD,
              "supermercado",
              "Supermercado",
              "RULE_SUPERMARKET",
              Confidence.HIGH
      );
    }

    if (contains(text, "gastrobus")) {
      return result(
              MoneyTransaction.MovementType.EXPENSE,
              MoneyTransaction.BalanceImpact.UNKNOWN,
              paymentChannel,
              null,
              null,
              MoneyTransaction.ClassificationStatus.REVIEW,
              "RULE_GASTROBUS_AMBIGUOUS",
              Confidence.LOW,
              RowStatus.REVIEW,
              "GASTROBUS es ambiguo entre alimentación/transporte. Requiere confirmación manual."
      );
    }

    if (contains(text, "mercado credito") || contains(text, "mercadocredito")) {
      return result(
              MoneyTransaction.MovementType.EXPENSE,
              MoneyTransaction.BalanceImpact.DEBT_OUTFLOW,
              paymentChannel,
              "mercadocredito",
              "Mercado Crédito",
              MoneyTransaction.ClassificationStatus.CLASSIFIED,
              "RULE_MERCADO_CREDITO_DEBT_PAYMENT",
              Confidence.HIGH,
              RowStatus.READY,
              null
      );
    }

    if (contains(text, "credito traspaso cajero autom")
            || contains(text, "traspaso")) {
      return result(
              MoneyTransaction.MovementType.TRANSFER,
              MoneyTransaction.BalanceImpact.INTERNAL_TRANSFER,
              MoneyTransaction.PaymentChannel.INTERNAL_TRANSFER,
              "transferenciaentrecuentas",
              "Transferencia entre cuentas",
              MoneyTransaction.ClassificationStatus.TECHNICAL,
              "RULE_INTERNAL_TRANSFER_TRASPASO",
              Confidence.MEDIUM,
              RowStatus.REVIEW,
              "Traspaso detectado. Revisar cuenta contraparte antes de confirmarlo."
      );
    }

    if (paymentChannel == MoneyTransaction.PaymentChannel.DEBIN
            || paymentChannel == MoneyTransaction.PaymentChannel.CUENTA_DNI
            || contains(text, "transferencia cdni")
            || contains(text, "credito cuenta dni")
            || contains(text, "debito cuenta dni")) {
      return result(
              defaultMovement,
              MoneyTransaction.BalanceImpact.UNKNOWN,
              paymentChannel,
              null,
              null,
              MoneyTransaction.ClassificationStatus.REVIEW,
              "RULE_CHANNEL_TRANSFER_REVIEW",
              Confidence.LOW,
              RowStatus.REVIEW,
              "DEBIN/Cuenta DNI identifica canal, no categoría económica. Revisar si es transferencia propia o gasto/ingreso real."
      );
    }

    if (contains(text, "pago con tarjeta debito")) {
      return result(
              MoneyTransaction.MovementType.EXPENSE,
              MoneyTransaction.BalanceImpact.CONSUMPTION_EXPENSE,
              MoneyTransaction.PaymentChannel.DEBIT_CARD,
              null,
              null,
              MoneyTransaction.ClassificationStatus.REVIEW,
              "RULE_DEBIT_CARD_PURCHASE_REVIEW",
              Confidence.LOW,
              RowStatus.REVIEW,
              "Compra con débito sin comercio categorizable. Requiere categoría."
      );
    }

    if (contains(text, "debito pago directo") || contains(text, "bk cobranzas")) {
      return result(
              MoneyTransaction.MovementType.EXPENSE,
              MoneyTransaction.BalanceImpact.UNKNOWN,
              paymentChannel,
              null,
              null,
              MoneyTransaction.ClassificationStatus.REVIEW,
              "RULE_DIRECT_DEBIT_REVIEW",
              Confidence.LOW,
              RowStatus.REVIEW,
              "Débito directo sin regla específica. Revisar antes de clasificarlo como gasto real."
      );
    }

    return result(
            defaultMovement,
            MoneyTransaction.BalanceImpact.UNKNOWN,
            paymentChannel,
            null,
            null,
            MoneyTransaction.ClassificationStatus.NEEDS_CATEGORY,
            "NO_IMPORT_RULE",
            Confidence.LOW,
            RowStatus.NEEDS_CATEGORY,
            "No hay regla confiable para categorizar este movimiento."
    );
  }

  public MoneyTransaction.PaymentChannel inferMercadoPagoPaymentChannel(
          String paymentMethodType,
          String paymentMethod
  ) {
    var text = normalizedJoin(paymentMethodType, paymentMethod);

    if (contains(text, "moneda digital") && contains(text, "prestamos")) {
      return MoneyTransaction.PaymentChannel.MERCADO_CREDITO;
    }

    if (contains(text, "transferencia bancaria") && contains(text, "debito inmediato")) {
      return MoneyTransaction.PaymentChannel.DEBIN;
    }

    if (contains(text, "transferencia bancaria") && contains(text, "cuenta bancaria digital")) {
      return MoneyTransaction.PaymentChannel.BANK_TRANSFER;
    }

    if (contains(text, "available money") || contains(text, "dinero disponible")) {
      return MoneyTransaction.PaymentChannel.MERCADO_PAGO;
    }

    return MoneyTransaction.PaymentChannel.MERCADO_PAGO;
  }

  public MoneyTransaction.PaymentChannel inferBancoProvinciaPaymentChannel(
          String description,
          String extendedDescription
  ) {
    var text = normalizedJoin(description, extendedDescription);

    if (contains(text, "debin")) {
      return MoneyTransaction.PaymentChannel.DEBIN;
    }

    if (contains(text, "cuenta dni") || contains(text, "cdni")) {
      return MoneyTransaction.PaymentChannel.CUENTA_DNI;
    }

    if (contains(text, "tarjeta debito")) {
      return MoneyTransaction.PaymentChannel.DEBIT_CARD;
    }

    if (contains(text, "credito haberes")
            || contains(text, "transferencia")
            || contains(text, "traspaso")
            || contains(text, "acreditacion intereses")) {
      return MoneyTransaction.PaymentChannel.BANK_TRANSFER;
    }

    return MoneyTransaction.PaymentChannel.UNKNOWN;
  }

  private ImportClassificationResult expense(
          MoneyTransaction.PaymentChannel paymentChannel,
          String categoryKey,
          String categoryName,
          String reason,
          Confidence confidence
  ) {
    return result(
            MoneyTransaction.MovementType.EXPENSE,
            MoneyTransaction.BalanceImpact.CONSUMPTION_EXPENSE,
            paymentChannel,
            categoryKey,
            categoryName,
            MoneyTransaction.ClassificationStatus.CLASSIFIED,
            reason,
            confidence,
            RowStatus.READY,
            null
    );
  }

  private ImportClassificationResult result(
          MoneyTransaction.MovementType movementType,
          MoneyTransaction.BalanceImpact balanceImpact,
          MoneyTransaction.PaymentChannel paymentChannel,
          String categoryKey,
          String categoryName,
          MoneyTransaction.ClassificationStatus classificationStatus,
          String classificationReason,
          Confidence confidence,
          RowStatus rowStatus,
          String warning
  ) {
    return new ImportClassificationResult(
            movementType,
            balanceImpact,
            paymentChannel,
            categoryKey,
            categoryName,
            classificationStatus,
            classificationReason,
            confidence,
            rowStatus,
            warning
    );
  }

  private boolean isSmallMercadoPagoTechnicalMovement(
          BigDecimal signedAmount,
          String rawDescription,
          String paymentMethodType,
          String paymentMethod,
          String payer,
          String liquidated
  ) {
    var amount = signedAmount == null ? BigDecimal.ZERO : signedAmount.abs();
    var hasUsefulText = !normalizer.normalize(rawDescription).isBlank()
            || !normalizer.normalize(paymentMethodType).isBlank()
            || !normalizer.normalize(paymentMethod).isBlank()
            || !normalizer.normalize(payer).isBlank();

    return amount.compareTo(new BigDecimal("5.00")) < 0
            && !hasUsefulText
            && "false".equalsIgnoreCase(normalizer.cleanRaw(liquidated));
  }

  private boolean contains(String haystack, String needle) {
    return haystack.contains(normalizer.normalize(needle));
  }

  private String normalizedJoin(String... values) {
    if (values == null || values.length == 0) {
      return "";
    }

    var builder = new StringBuilder();

    for (var value : values) {
      if (value != null && !value.isBlank()) {
        if (!builder.isEmpty()) {
          builder.append(' ');
        }
        builder.append(value);
      }
    }

    return normalizer.normalize(builder.toString());
  }
}
