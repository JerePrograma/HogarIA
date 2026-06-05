package com.hogaria.service.transactionimport;

import static com.hogaria.service.transactionimport.ImportClassificationResults.expense;
import static com.hogaria.service.transactionimport.ImportClassificationResults.result;
import static com.hogaria.service.transactionimport.ImportClassificationResults.review;

import com.hogaria.dto.TransactionImportDtos.Confidence;
import com.hogaria.dto.TransactionImportDtos.RowStatus;
import com.hogaria.dto.TransactionImportDtos.TransactionImportSource;
import com.hogaria.entity.MoneyTransaction;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

final class MercadoPagoImportRules {

  private static final BigDecimal SMALL_YIELD_LIMIT = new BigDecimal("100.00");

  private MercadoPagoImportRules() {
  }

  static List<ImportClassificationRule> build(ImportTextNormalizer normalizer) {
    var rules = new ArrayList<ImportClassificationRule>();

    rule(rules, ClassificationLayer.SOURCE_SPECIFIC, 10, "RULE_MP_YIELD_EMPTY_DETAIL", "rawDescription",
            MercadoPagoImportRules::isEmptyDetailYield,
            movement -> result(
                    movement,
                    ClassificationLayer.SOURCE_SPECIFIC,
                    "rawDescription",
                    movement.rawDescription(),
                    MoneyTransaction.MovementType.INCOME,
                    MoneyTransaction.BalanceImpact.INTEREST_INCOME,
                    MoneyTransaction.PaymentChannel.MONEY_MARKET_YIELD,
                    "rendimientomercadopago",
                    "Rendimiento Mercado Pago",
                    MoneyTransaction.ClassificationStatus.CLASSIFIED,
                    "RULE_MP_YIELD_EMPTY_DETAIL",
                    Confidence.HIGH,
                    RowStatus.READY,
                    null
            ));

    rule(rules, ClassificationLayer.SOURCE_SPECIFIC, 20, "RULE_MP_EMOVA_TRANSPORT", "identificationNumber",
            movement -> containsAny(normalizer, movement, "identificationNumber", "emova")
                    || containsAny(normalizer, movement, "operationTags", "emova"),
            movement -> expense(
                    movement,
                    ClassificationLayer.SOURCE_SPECIFIC,
                    "identificationNumber",
                    firstNonBlank(movement.identificationNumber(), movement.operationTags()),
                    MoneyTransaction.PaymentChannel.TRANSPORT_CARD,
                    "transportepublico",
                    "Transporte público",
                    "RULE_MP_EMOVA_TRANSPORT",
                    Confidence.HIGH
            ));

    rule(rules, ClassificationLayer.SOURCE_SPECIFIC, 30, "RULE_MP_REFUND", "operationType",
            movement -> containsAny(normalizer, movement, "operationType", "devolucion", "reembolso", "refund"),
            movement -> result(
                    movement,
                    ClassificationLayer.SOURCE_SPECIFIC,
                    "operationType",
                    movement.operationType(),
                    MoneyTransaction.MovementType.ADJUSTMENT,
                    MoneyTransaction.BalanceImpact.REFUND_OR_REIMBURSEMENT,
                    movement.paymentChannel(),
                    "reintegrosydevoluciones",
                    "Reintegros y devoluciones",
                    MoneyTransaction.ClassificationStatus.CLASSIFIED,
                    "RULE_MP_REFUND",
                    Confidence.HIGH,
                    RowStatus.READY,
                    "Devolución detectada: no se cuenta como ingreso operativo ni como consumo."
            ));

    text(rules, 100, "RULE_MP_SUBE_TRANSPORT", new String[]{"sube", "carga tarjeta sube", "pasajes"}, "transportepublico", "Transporte público", MoneyTransaction.PaymentChannel.TRANSPORT_CARD);
    text(rules, 110, "RULE_MP_UBER", new String[]{"uber"}, "taxiyapps", "Taxi y apps", MoneyTransaction.PaymentChannel.MERCADO_PAGO);
    text(rules, 120, "RULE_MP_DIA_SUPERMARKET", new String[]{"compra en dia", "dia"}, "supermercado", "Supermercado", MoneyTransaction.PaymentChannel.MERCADO_PAGO);
    text(rules, 130, "RULE_MP_SHELL_BOX", new String[]{"shell box"}, "combustible", "Combustible", MoneyTransaction.PaymentChannel.MERCADO_PAGO);
    text(rules, 140, "RULE_MP_EDEA", new String[]{"edea"}, "electricidad", "Electricidad", MoneyTransaction.PaymentChannel.MERCADO_PAGO);
    text(rules, 150, "RULE_MP_CAMUZZI", new String[]{"camuzzi gas pampeana", "camuzzi"}, "gas", "Gas", MoneyTransaction.PaymentChannel.MERCADO_PAGO);
    text(rules, 160, "RULE_MP_MELI_PLUS", new String[]{"suscripcion a meli", "suscripción a meli", "meli+"}, "meli", "Meli+", MoneyTransaction.PaymentChannel.MERCADO_PAGO);
    text(rules, 170, "RULE_MP_TUENTI", new String[]{"tuenti", "cellphone_mla_tuenti"}, "telefoniamovil", "Telefonía móvil", MoneyTransaction.PaymentChannel.MERCADO_PAGO);

    rule(rules, ClassificationLayer.STRONG_TEXT_PATTERN, 200, "RULE_MP_MERCADO_CREDITO_DEBT", "normalizedDescription",
            movement -> containsAny(normalizer, movement, "normalizedDescription",
                    "payment linked to a loan origination",
                    "pago de cuotas de mercado credito",
                    "pago de creditos de mercado pago",
                    "pago de créditos de mercado pago",
                    "mercadocredito",
                    "mercado credito")
                    || containsAny(normalizer, movement, "payer", "mercadocredito", "mercado credito"),
            movement -> {
              var isPayment = movement.amount() != null && movement.amount().signum() < 0;
              return result(
                      movement,
                      ClassificationLayer.STRONG_TEXT_PATTERN,
                      "normalizedDescription",
                      firstNonBlank(movement.rawDescription(), movement.extendedDescription()),
                      isPayment ? MoneyTransaction.MovementType.EXPENSE : MoneyTransaction.MovementType.ADJUSTMENT,
                      isPayment ? MoneyTransaction.BalanceImpact.DEBT_OUTFLOW : MoneyTransaction.BalanceImpact.NEUTRAL_ADJUSTMENT,
                      MoneyTransaction.PaymentChannel.MERCADO_CREDITO,
                      "mercadocredito",
                      "Mercado Crédito",
                      MoneyTransaction.ClassificationStatus.CLASSIFIED,
                      "RULE_MP_MERCADO_CREDITO_DEBT",
                      Confidence.HIGH,
                      RowStatus.READY,
                      isPayment ? null : "Originación de deuda: se clasifica como deuda/ajuste, no como ingreso operativo."
              );
            });

    rule(rules, ClassificationLayer.SOURCE_SPECIFIC, 300, "RULE_MP_FUNDING_TRANSFER", "rawDescription",
            movement -> containsAny(normalizer, movement, "normalizedDescription",
                    "pago debin",
                    "bank transfer",
                    "transferencia bancaria",
                    "cuenta bancaria digital",
                    "debito inmediato",
                    "débito inmediato"),
            movement -> result(
                    movement,
                    ClassificationLayer.SOURCE_SPECIFIC,
                    "normalizedDescription",
                    firstNonBlank(movement.rawDescription(), movement.extendedDescription()),
                    MoneyTransaction.MovementType.TRANSFER,
                    MoneyTransaction.BalanceImpact.INTERNAL_TRANSFER,
                    movement.paymentChannel(),
                    "fondeomercadopago",
                    "Fondeo Mercado Pago",
                    MoneyTransaction.ClassificationStatus.TECHNICAL,
                    "RULE_MP_FUNDING_TRANSFER",
                    Confidence.MEDIUM,
                    RowStatus.REVIEW,
                    "Fondeo/transferencia de Mercado Pago. No impacta como ingreso operativo."
            ));

    rule(rules, ClassificationLayer.SOURCE_SPECIFIC, 310, "RULE_MP_PAYMENT_LINK_REVIEW", "rawDescription",
            movement -> containsAny(normalizer, movement, "rawDescription", "link de pago"),
            movement -> review(
                    movement,
                    ClassificationLayer.SOURCE_SPECIFIC,
                    "rawDescription",
                    movement.rawDescription(),
                    MoneyTransaction.MovementType.INCOME,
                    MoneyTransaction.BalanceImpact.UNKNOWN,
                    movement.paymentChannel(),
                    "cobrosporlink",
                    "Cobros por link",
                    "RULE_MP_PAYMENT_LINK_REVIEW",
                    Confidence.MEDIUM,
                    "Puede ser recupero, venta o transferencia. Si el perfil tiene sincronización CJ configurada, puede mapearse a capital recuperado."
            ));

    rule(rules, ClassificationLayer.SOURCE_SPECIFIC, 320, "RULE_MP_GENERIC_DETAIL_REVIEW", "rawDescription",
            movement -> containsAny(normalizer, movement, "rawDescription", "varios", "var", "orden de venta"),
            movement -> review(
                    movement,
                    ClassificationLayer.SOURCE_SPECIFIC,
                    "rawDescription",
                    movement.rawDescription(),
                    defaultMovement(movement),
                    MoneyTransaction.BalanceImpact.UNKNOWN,
                    movement.paymentChannel(),
                    null,
                    null,
                    "RULE_MP_GENERIC_DETAIL_REVIEW",
                    Confidence.LOW,
                    "Detalle genérico. Usar historial confirmado o revisión manual; no se autoclasifica por pagador."
            ));

    rule(rules, ClassificationLayer.GENERIC_FALLBACK, 900, "NO_IMPORT_RULE", "normalizedDescription",
            movement -> true,
            movement -> new ImportClassificationResult(
                    defaultMovement(movement),
                    MoneyTransaction.BalanceImpact.UNKNOWN,
                    movement.paymentChannel(),
                    null,
                    null,
                    MoneyTransaction.ClassificationStatus.NEEDS_CATEGORY,
                    "NO_IMPORT_RULE",
                    Confidence.LOW,
                    RowStatus.NEEDS_CATEGORY,
                    "No hay regla confiable para categorizar este movimiento.",
                    ClassificationLayer.GENERIC_FALLBACK,
                    "normalizedDescription",
                    movement.normalizedDescription(),
                    null
            ));

    return rules;
  }

  private static void text(
          List<ImportClassificationRule> rules,
          int priority,
          String reasonCode,
          String[] needles,
          String categoryKey,
          String categoryName,
          MoneyTransaction.PaymentChannel paymentChannel
  ) {
    rule(rules, ClassificationLayer.MERCHANT_ALIAS, priority, reasonCode, "rawDescription",
            movement -> containsAny(new ImportTextNormalizer(), movement, "rawDescription", needles),
            movement -> expense(
                    movement,
                    ClassificationLayer.MERCHANT_ALIAS,
                    "rawDescription",
                    movement.rawDescription(),
                    paymentChannel,
                    categoryKey,
                    categoryName,
                    reasonCode,
                    Confidence.HIGH
            ));
  }

  private static void rule(
          List<ImportClassificationRule> rules,
          ClassificationLayer layer,
          int priority,
          String reasonCode,
          String fieldName,
          java.util.function.Predicate<NormalizedImportMovement> predicate,
          Function<NormalizedImportMovement, ImportClassificationResult> result
  ) {
    rules.add(new ImportClassificationRule(
            TransactionImportSource.MERCADO_PAGO,
            layer,
            priority,
            reasonCode,
            fieldName,
            predicate,
            result
    ));
  }

  private static boolean isEmptyDetailYield(NormalizedImportMovement movement) {
    var amount = movement.amount() == null ? BigDecimal.ZERO : movement.amount();
    return amount.signum() > 0
            && amount.compareTo(SMALL_YIELD_LIMIT) < 0
            && isBlank(movement.rawDescription())
            && isBlank(movement.paymentMethodType())
            && isBlank(movement.paymentMethod())
            && "false".equalsIgnoreCase(firstNonBlank(movement.liquidated()));
  }

  private static boolean containsAny(
          ImportTextNormalizer normalizer,
          NormalizedImportMovement movement,
          String fieldName,
          String... needles
  ) {
    var haystack = normalizer.normalize(movement.fieldValue(fieldName));

    for (var needle : needles) {
      if (haystack.contains(normalizer.normalize(needle))) {
        return true;
      }
    }

    return false;
  }

  private static MoneyTransaction.MovementType defaultMovement(NormalizedImportMovement movement) {
    return movement.amount() != null && movement.amount().signum() < 0
            ? MoneyTransaction.MovementType.EXPENSE
            : MoneyTransaction.MovementType.INCOME;
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return "";
    }

    for (var value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }

    return "";
  }
}
