package com.hogaria.service.transactionimport;

import static com.hogaria.service.transactionimport.ImportClassificationResults.expense;
import static com.hogaria.service.transactionimport.ImportClassificationResults.result;
import static com.hogaria.service.transactionimport.ImportClassificationResults.review;

import com.hogaria.dto.TransactionImportDtos.Confidence;
import com.hogaria.dto.TransactionImportDtos.RowStatus;
import com.hogaria.dto.TransactionImportDtos.TransactionImportSource;
import com.hogaria.entity.MoneyTransaction;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

final class BancoProvinciaImportRules {

  private BancoProvinciaImportRules() {
  }

  static List<ImportClassificationRule> build(ImportTextNormalizer normalizer) {
    var rules = new ArrayList<ImportClassificationRule>();

    source(rules, 10, "RULE_SALARY_POLICIA", "extendedDescription",
            movement -> containsAny(normalizer, movement, "rawDescription", "credito haberes")
                    && containsAny(normalizer, movement, "extendedDescription", "policia"),
            movement -> result(
                    movement,
                    ClassificationLayer.SOURCE_SPECIFIC,
                    "extendedDescription",
                    movement.extendedDescription(),
                    MoneyTransaction.MovementType.INCOME,
                    MoneyTransaction.BalanceImpact.OPERATING_INCOME,
                    movement.paymentChannel(),
                    "sueldo",
                    "Sueldo",
                    MoneyTransaction.ClassificationStatus.CLASSIFIED,
                    "RULE_SALARY_POLICIA",
                    Confidence.HIGH,
                    RowStatus.READY,
                    null
            ));

    source(rules, 20, "RULE_INTEREST_INCOME", "rawDescription",
            movement -> containsAny(normalizer, movement, "rawDescription", "acreditacion intereses", "acredit intereses"),
            movement -> result(
                    movement,
                    ClassificationLayer.SOURCE_SPECIFIC,
                    "rawDescription",
                    movement.rawDescription(),
                    MoneyTransaction.MovementType.INCOME,
                    MoneyTransaction.BalanceImpact.INTEREST_INCOME,
                    movement.paymentChannel(),
                    "interesesyrendimientos",
                    "Intereses y rendimientos",
                    MoneyTransaction.ClassificationStatus.CLASSIFIED,
                    "RULE_INTEREST_INCOME",
                    Confidence.HIGH,
                    RowStatus.READY,
                    null
            ));

    source(rules, 21, "RULE_SYSTEMSCORP_OPERATING_INCOME", "extendedDescription",
            movement -> isPositive(movement)
                    && containsAny(normalizer, movement, "rawDescription", "credito traspaso cajero autom")
                    && containsAny(normalizer, movement, "extendedDescription", "systemscorp sa"),
            movement -> result(
                    movement,
                    ClassificationLayer.SOURCE_SPECIFIC,
                    "extendedDescription",
                    movement.extendedDescription(),
                    MoneyTransaction.MovementType.INCOME,
                    MoneyTransaction.BalanceImpact.OPERATING_INCOME,
                    movement.paymentChannel(),
                    "ingresosporserviciosyproyectos",
                    "Ingresos por servicios y proyectos",
                    MoneyTransaction.ClassificationStatus.CLASSIFIED,
                    "RULE_SYSTEMSCORP_OPERATING_INCOME",
                    Confidence.HIGH,
                    RowStatus.READY,
                    null
            ));

    source(rules, 22, "RULE_BP_INCOMING_TRANSFER_REVIEW", "rawDescription",
            movement -> isPositive(movement)
                    && containsAny(normalizer, movement, "rawDescription",
                    "credito traspaso cajero autom",
                    "credito cuenta dni",
                    "credito debin",
                    "credito transferencia i"),
            movement -> review(
                    movement,
                    ClassificationLayer.SOURCE_SPECIFIC,
                    "rawDescription",
                    movement.rawDescription(),
                    MoneyTransaction.MovementType.INCOME,
                    MoneyTransaction.BalanceImpact.UNKNOWN,
                    movement.paymentChannel(),
                    "transferenciasrecibidas",
                    "Transferencias recibidas",
                    "RULE_BP_INCOMING_TRANSFER_REVIEW",
                    Confidence.LOW,
                    "Transferencia entrante externa: puede ser ingreso, recupero, préstamo, devolución o transferencia familiar."
            ));

    source(rules, 30, "RULE_BENEFIT_REIMBURSEMENT", "rawDescription",
            movement -> containsAny(normalizer, movement, "rawDescription", "benef pei cuenta dni", "beneficio", "benef "),
            movement -> result(
                    movement,
                    ClassificationLayer.SOURCE_SPECIFIC,
                    "rawDescription",
                    movement.rawDescription(),
                    MoneyTransaction.MovementType.INCOME,
                    MoneyTransaction.BalanceImpact.REFUND_OR_REIMBURSEMENT,
                    movement.paymentChannel(),
                    "beneficiosypromociones",
                    "Beneficios y promociones",
                    MoneyTransaction.ClassificationStatus.CLASSIFIED,
                    "RULE_BENEFIT_REIMBURSEMENT",
                    Confidence.HIGH,
                    RowStatus.READY,
                    null
            ));

    source(rules, 40, "RULE_TAX_RG4815", "rawDescription",
            movement -> containsAny(normalizer, movement, "rawDescription", "impuesto rg 4815"),
            movement -> expense(
                    movement,
                    ClassificationLayer.SOURCE_SPECIFIC,
                    "rawDescription",
                    movement.rawDescription(),
                    movement.paymentChannel(),
                    "percepcionesrg4815",
                    "Percepciones RG 4815",
                    "RULE_TAX_RG4815",
                    Confidence.HIGH
            ));

    source(rules, 50, "RULE_BANCO_PROVINCIA_LOAN_PAYMENT", "rawDescription",
            movement -> containsAny(normalizer, movement, "rawDescription", "pago cuota de prestamo", "amortizacion extra prestamo")
                    || containsAny(normalizer, movement, "extendedDescription", "pago cuota de prestamo", "amortizacion extra prestamo"),
            movement -> result(
                    movement,
                    ClassificationLayer.SOURCE_SPECIFIC,
                    "rawDescription",
                    movement.rawDescription(),
                    MoneyTransaction.MovementType.EXPENSE,
                    MoneyTransaction.BalanceImpact.DEBT_OUTFLOW,
                    movement.paymentChannel(),
                    "prestamobancoprovincia",
                    "Préstamo Banco Provincia",
                    MoneyTransaction.ClassificationStatus.CLASSIFIED,
                    "RULE_BANCO_PROVINCIA_LOAN_PAYMENT",
                    Confidence.HIGH,
                    RowStatus.READY,
                    null
            ));

    source(rules, 60, "RULE_BP_REFUND_COVERAGE", "rawDescription",
            movement -> isPositive(movement)
                    && (containsAny(normalizer, movement, "rawDescription", "devolucion", "credito cobertura")
                    || containsAny(normalizer, movement, "extendedDescription", "devolucion")),
            movement -> result(
                    movement,
                    ClassificationLayer.SOURCE_SPECIFIC,
                    "rawDescription",
                    movement.rawDescription(),
                    MoneyTransaction.MovementType.INCOME,
                    MoneyTransaction.BalanceImpact.REFUND_OR_REIMBURSEMENT,
                    movement.paymentChannel(),
                    "beneficiosypromociones",
                    "Beneficios y promociones",
                    MoneyTransaction.ClassificationStatus.CLASSIFIED,
                    "RULE_BP_REFUND_COVERAGE",
                    Confidence.HIGH,
                    RowStatus.READY,
                    "Devolución/reintegro: no es ingreso operativo."
            ));

    source(rules, 70, "RULE_BP_CASH_WITHDRAWAL", "rawDescription",
            movement -> isNegative(movement)
                    && containsAny(normalizer, movement, "rawDescription", "extrac.fondos cajero autom", "extrac fondos cajero autom"),
            movement -> review(
                    movement,
                    ClassificationLayer.SOURCE_SPECIFIC,
                    "rawDescription",
                    movement.rawDescription(),
                    MoneyTransaction.MovementType.TRANSFER,
                    MoneyTransaction.BalanceImpact.CASH_WITHDRAWAL,
                    MoneyTransaction.PaymentChannel.ATM,
                    null,
                    null,
                    "RULE_BP_CASH_WITHDRAWAL",
                    Confidence.MEDIUM,
                    "Retiro de efectivo. Vincular a una cuenta de caja o revisar; no se cuenta como consumo por sí mismo."
            ));

    merchant(rules, 100, "RULE_MERCHANT_UBER", "PAYU*AR*UBER", "taxiyapps", "Taxi y apps", MoneyTransaction.PaymentChannel.DEBIT_CARD);
    merchant(rules, 110, "RULE_MERCHANT_SUBE", "MERPAGO*SUBE", "transportepublico", "Transporte público", MoneyTransaction.PaymentChannel.TRANSPORT_CARD);
    merchant(rules, 120, "RULE_MERCHANT_BUSPLUS", "BUSPLUS", "transportepublico", "Transporte público", MoneyTransaction.PaymentChannel.TRANSPORT_CARD);
    merchant(rules, 130, "RULE_MERCHANT_DIA", "DIA TIENDA", "supermercado", "Supermercado", MoneyTransaction.PaymentChannel.DEBIT_CARD);
    merchant(rules, 131, "RULE_MERCHANT_SUPERDIA", "MERPAGO*SUPERDIA", "supermercado", "Supermercado", MoneyTransaction.PaymentChannel.DEBIT_CARD);
    merchant(rules, 140, "RULE_MERCHANT_PEDIDOSYA", "PEDIDOSYA", "deliveryyrestaurantes", "Delivery y restaurantes", MoneyTransaction.PaymentChannel.DEBIT_CARD);
    merchant(rules, 141, "RULE_MERCHANT_MOSTAZA", "MERPAGO*MOSTAZA", "deliveryyrestaurantes", "Delivery y restaurantes", MoneyTransaction.PaymentChannel.DEBIT_CARD);
    merchant(rules, 150, "RULE_MERCHANT_TUENTI", "MERPAGO*TUENTI", "telefoniamovil", "Telefonía móvil", MoneyTransaction.PaymentChannel.DEBIT_CARD);

    rule(rules, ClassificationLayer.MERCHANT_ALIAS, 160, "RULE_MERCHANT_EBANX_REVIEW", "merchant",
            movement -> containsAny(normalizer, movement, "merchantNormalized", "MERPAGO*EBANXSA", "EBANXSA"),
            movement -> review(
                    movement,
                    ClassificationLayer.MERCHANT_ALIAS,
                    "merchant",
                    movement.merchantRaw(),
                    MoneyTransaction.MovementType.EXPENSE,
                    MoneyTransaction.BalanceImpact.CONSUMPTION_EXPENSE,
                    MoneyTransaction.PaymentChannel.DEBIT_CARD,
                    "plataformasdigitales",
                    "Plataformas digitales",
                    "RULE_MERCHANT_EBANX_REVIEW",
                    Confidence.MEDIUM,
                    "EBANX suele ser pasarela de plataformas digitales, pero requiere confirmar alias de perfil."
            ));

    source(rules, 200, "RULE_ARCA_AFIP_MONOTRIBUTO", "extendedDescription",
            movement -> containsAny(normalizer, movement, "extendedDescription", "arca", "afip", "monotrib")
                    || containsAny(normalizer, movement, "rawDescription", "arca", "afip", "monotrib"),
            movement -> expense(
                    movement,
                    ClassificationLayer.SOURCE_SPECIFIC,
                    "extendedDescription",
                    movement.extendedDescription(),
                    MoneyTransaction.PaymentChannel.DIRECT_DEBIT,
                    "monotributo",
                    "Monotributo",
                    "RULE_ARCA_AFIP_MONOTRIBUTO",
                    Confidence.HIGH
            ));

    source(rules, 210, "RULE_DIRECT_DEBIT_INSURANCE", "extendedDescription",
            movement -> containsAny(normalizer, movement, "extendedDescription", "caja de seg"),
            movement -> expense(
                    movement,
                    ClassificationLayer.SOURCE_SPECIFIC,
                    "extendedDescription",
                    movement.extendedDescription(),
                    MoneyTransaction.PaymentChannel.DIRECT_DEBIT,
                    "seguros",
                    "Seguros",
                    "RULE_DIRECT_DEBIT_INSURANCE",
                    Confidence.HIGH
            ));

    source(rules, 220, "RULE_DIRECT_DEBIT_RECURRENT_REVIEW", "extendedDescription",
            movement -> containsAny(normalizer, movement, "extendedDescription", "bk cobranzas", "tertium sa", "asociacion mutu"),
            movement -> review(
                    movement,
                    ClassificationLayer.SOURCE_SPECIFIC,
                    "extendedDescription",
                    movement.extendedDescription(),
                    MoneyTransaction.MovementType.EXPENSE,
                    MoneyTransaction.BalanceImpact.UNKNOWN,
                    MoneyTransaction.PaymentChannel.DIRECT_DEBIT,
                    "cobranzarecurrentearevisar",
                    "Cobranza recurrente a revisar",
                    "RULE_DIRECT_DEBIT_RECURRENT_REVIEW",
                    Confidence.MEDIUM,
                    "Débito recurrente sensible: se deja en revisión salvo alias específico del perfil."
            ));

    source(rules, 300, "RULE_CDNI_EL_ABASTO", "merchant",
            movement -> containsAny(normalizer, movement, "merchantNormalized", "el abasto"),
            movement -> expense(
                    movement,
                    ClassificationLayer.SOURCE_SPECIFIC,
                    "merchant",
                    movement.merchantRaw(),
                    MoneyTransaction.PaymentChannel.CUENTA_DNI,
                    "almacen",
                    "Almacén",
                    "RULE_CDNI_EL_ABASTO",
                    Confidence.HIGH
            ));

    source(rules, 310, "RULE_CDNI_DELIVERY_RESTAURANT", "merchant",
            movement -> containsAny(normalizer, movement, "merchantNormalized", "la posta de miramar", "gastrobus"),
            movement -> expense(
                    movement,
                    ClassificationLayer.SOURCE_SPECIFIC,
                    "merchant",
                    movement.merchantRaw(),
                    MoneyTransaction.PaymentChannel.CUENTA_DNI,
                    "deliveryyrestaurantes",
                    "Delivery y restaurantes",
                    "RULE_CDNI_DELIVERY_RESTAURANT",
                    Confidence.HIGH
            ));

    fallback(rules, 870, "RULE_CDNI_PURCHASE_REVIEW",
            movement -> isNegative(movement)
                    && containsAny(normalizer, movement, "rawDescription",
                    "pago con transferencia cdni",
                    "compra con transferencia cdni"),
            movement -> review(
                    movement,
                    ClassificationLayer.GENERIC_FALLBACK,
                    "rawDescription",
                    movement.rawDescription(),
                    MoneyTransaction.MovementType.EXPENSE,
                    MoneyTransaction.BalanceImpact.CONSUMPTION_EXPENSE,
                    MoneyTransaction.PaymentChannel.CUENTA_DNI,
                    null,
                    null,
                    "RULE_CDNI_PURCHASE_REVIEW",
                    Confidence.LOW,
                    "Compra real por transferencia Cuenta DNI pendiente de categoría."
            ));

    fallback(rules, 880, "RULE_FOREIGN_CURRENCY_DEBIT_PURCHASE_REVIEW",
            movement -> isNegative(movement)
                    && containsAny(normalizer, movement, "rawDescription", "pago con t.d. en m.e."),
            movement -> review(
                    movement,
                    ClassificationLayer.GENERIC_FALLBACK,
                    "rawDescription",
                    movement.rawDescription(),
                    MoneyTransaction.MovementType.EXPENSE,
                    MoneyTransaction.BalanceImpact.CONSUMPTION_EXPENSE,
                    MoneyTransaction.PaymentChannel.CARD_FOREIGN_CURRENCY,
                    null,
                    null,
                    "RULE_FOREIGN_CURRENCY_DEBIT_PURCHASE_REVIEW",
                    Confidence.LOW,
                    "Consumo con tarjeta en moneda extranjera pendiente de categoría."
            ));

    fallback(rules, 890, "RULE_BP_OUTGOING_BANK_TRANSFER_REVIEW",
            movement -> isNegative(movement)
                    && containsAny(normalizer, movement, "rawDescription", "bip db transferencia"),
            movement -> review(
                    movement,
                    ClassificationLayer.GENERIC_FALLBACK,
                    "rawDescription",
                    movement.rawDescription(),
                    MoneyTransaction.MovementType.TRANSFER,
                    MoneyTransaction.BalanceImpact.EXTERNAL_TRANSFER,
                    MoneyTransaction.PaymentChannel.BANK_TRANSFER,
                    null,
                    null,
                    "RULE_BP_OUTGOING_BANK_TRANSFER_REVIEW",
                    Confidence.LOW,
                    "Transferencia bancaria saliente externa. No es interna sin alias propio confirmado."
            ));

    fallback(rules, 900, "RULE_CHANNEL_TRANSFER_REVIEW",
            movement -> movement.paymentChannel() == MoneyTransaction.PaymentChannel.DEBIN
                    || movement.paymentChannel() == MoneyTransaction.PaymentChannel.CUENTA_DNI
                    || containsAny(normalizer, movement, "rawDescription", "transferencia cdni", "credito cuenta dni", "debito cuenta dni", "debin"),
            movement -> review(
                    movement,
                    ClassificationLayer.GENERIC_FALLBACK,
                    "rawDescription",
                    movement.rawDescription(),
                    defaultMovement(movement),
                    MoneyTransaction.BalanceImpact.UNKNOWN,
                    movement.paymentChannel(),
                    null,
                    null,
                    "RULE_CHANNEL_TRANSFER_REVIEW",
                    Confidence.LOW,
                    "DEBIN/Cuenta DNI identifica canal, no categoría económica. Revisar contraparte o alias de perfil."
            ));

    fallback(rules, 910, "RULE_DEBIT_CARD_PURCHASE_REVIEW",
            movement -> containsAny(normalizer, movement, "rawDescription", "pago con tarjeta debito", "pago con t.d."),
            movement -> review(
                    movement,
                    ClassificationLayer.GENERIC_FALLBACK,
                    "rawDescription",
                    movement.rawDescription(),
                    MoneyTransaction.MovementType.EXPENSE,
                    MoneyTransaction.BalanceImpact.CONSUMPTION_EXPENSE,
                    MoneyTransaction.PaymentChannel.DEBIT_CARD,
                    null,
                    null,
                    "RULE_DEBIT_CARD_PURCHASE_REVIEW",
                    Confidence.LOW,
                    "Compra con débito sin comercio categorizable. Requiere categoría."
            ));

    fallback(rules, 920, "RULE_DIRECT_DEBIT_REVIEW",
            movement -> containsAny(normalizer, movement, "rawDescription", "debito pago directo"),
            movement -> review(
                    movement,
                    ClassificationLayer.GENERIC_FALLBACK,
                    "rawDescription",
                    movement.rawDescription(),
                    MoneyTransaction.MovementType.EXPENSE,
                    MoneyTransaction.BalanceImpact.UNKNOWN,
                    MoneyTransaction.PaymentChannel.DIRECT_DEBIT,
                    null,
                    null,
                    "RULE_DIRECT_DEBIT_REVIEW",
                    Confidence.LOW,
                    "Débito directo sin regla específica. Revisar antes de clasificarlo como gasto real."
            ));

    return rules;
  }

  private static void merchant(
          List<ImportClassificationRule> rules,
          int priority,
          String reasonCode,
          String alias,
          String categoryKey,
          String categoryName,
          MoneyTransaction.PaymentChannel paymentChannel
  ) {
    rule(rules, ClassificationLayer.MERCHANT_ALIAS, priority, reasonCode, "merchant",
            movement -> movement.merchantNormalized() != null
                    && movement.merchantNormalized().contains(new ImportTextNormalizer().normalize(alias)),
            movement -> expense(
                    movement,
                    ClassificationLayer.MERCHANT_ALIAS,
                    "merchant",
                    movement.merchantRaw(),
                    paymentChannel,
                    categoryKey,
                    categoryName,
                    reasonCode,
                    Confidence.HIGH
            ));
  }

  private static void source(
          List<ImportClassificationRule> rules,
          int priority,
          String reasonCode,
          String fieldName,
          java.util.function.Predicate<NormalizedImportMovement> predicate,
          Function<NormalizedImportMovement, ImportClassificationResult> result
  ) {
    rule(rules, ClassificationLayer.SOURCE_SPECIFIC, priority, reasonCode, fieldName, predicate, result);
  }

  private static void fallback(
          List<ImportClassificationRule> rules,
          int priority,
          String reasonCode,
          java.util.function.Predicate<NormalizedImportMovement> predicate,
          Function<NormalizedImportMovement, ImportClassificationResult> result
  ) {
    rule(rules, ClassificationLayer.GENERIC_FALLBACK, priority, reasonCode, "rawDescription", predicate, result);
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
            TransactionImportSource.BANCO_PROVINCIA,
            layer,
            priority,
            reasonCode,
            fieldName,
            predicate,
            result
    ));
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

  private static boolean isPositive(NormalizedImportMovement movement) {
    return movement.amount() != null && movement.amount().signum() > 0;
  }

  private static boolean isNegative(NormalizedImportMovement movement) {
    return movement.amount() != null && movement.amount().signum() < 0;
  }
}
