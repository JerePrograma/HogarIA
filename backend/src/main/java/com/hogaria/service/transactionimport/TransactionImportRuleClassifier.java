package com.hogaria.service.transactionimport;

import com.hogaria.dto.TransactionImportDtos.Confidence;
import com.hogaria.dto.TransactionImportDtos.RowStatus;
import com.hogaria.dto.TransactionImportDtos.TransactionImportSource;
import com.hogaria.entity.MoneyTransaction;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TransactionImportRuleClassifier {

  private final ImportTextNormalizer normalizer;
  private final PaymentChannelDetector paymentChannelDetector;
  private final List<ImportClassificationRule> rules;

  public TransactionImportRuleClassifier(ImportTextNormalizer normalizer) {
    this.normalizer = normalizer;
    this.paymentChannelDetector = new PaymentChannelDetector(normalizer);
    this.rules = java.util.stream.Stream
            .concat(
                    BancoProvinciaImportRules.build(normalizer).stream(),
                    MercadoPagoImportRules.build(normalizer).stream()
            )
            .sorted(Comparator
                    .comparing(ImportClassificationRule::source)
                    .thenComparing(rule -> rule.layer().ordinal())
                    .thenComparingInt(ImportClassificationRule::priority))
            .toList();
  }

  public ImportClassificationResult classify(NormalizedImportMovement movement) {
    return rules
            .stream()
            .filter(rule -> rule.matches(movement))
            .findFirst()
            .map(rule -> ensureExplanation(rule, movement, rule.result().apply(movement)))
            .orElseGet(() -> fallback(movement));
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
    return classifyMercadoPago(
            signedAmount,
            rawDescription,
            operationType,
            paymentMethodType,
            paymentMethod,
            payer,
            liquidated,
            "",
            "",
            paymentChannel
    );
  }

  public ImportClassificationResult classifyMercadoPago(
          BigDecimal signedAmount,
          String rawDescription,
          String operationType,
          String paymentMethodType,
          String paymentMethod,
          String payer,
          String liquidated,
          String operationTags,
          String identificationNumber,
          MoneyTransaction.PaymentChannel paymentChannel
  ) {
    var normalizedDescription = normalizer.normalize(normalizedJoin(
            rawDescription,
            operationType,
            paymentMethodType,
            paymentMethod,
            payer,
            operationTags,
            identificationNumber,
            liquidated
    ));

    return classify(new NormalizedImportMovement(
            TransactionImportSource.MERCADO_PAGO,
            null,
            null,
            null,
            null,
            signedAmount,
            "ARS",
            normalizer.cleanRaw(rawDescription),
            normalizedDescription,
            normalizedJoin(operationType, paymentMethodType, paymentMethod, payer, operationTags, identificationNumber, liquidated),
            null,
            null,
            payer,
            null,
            paymentChannel == null ? MoneyTransaction.PaymentChannel.MERCADO_PAGO : paymentChannel,
            operationKind(signedAmount),
            operationType,
            paymentMethodType,
            paymentMethod,
            liquidated,
            operationTags,
            identificationNumber,
            payer,
            null
    ));
  }

  public ImportClassificationResult classifyBancoProvincia(
          BigDecimal signedAmount,
          String description,
          String extendedDescription,
          String merchantName,
          MoneyTransaction.PaymentChannel paymentChannel,
          String counterparty
  ) {
    var merchantNormalized = normalizer.normalize(merchantName);
    var normalizedDescription = normalizer.normalize(normalizedJoin(description, extendedDescription, merchantName, counterparty));

    return classify(new NormalizedImportMovement(
            TransactionImportSource.BANCO_PROVINCIA,
            null,
            null,
            null,
            null,
            signedAmount,
            "ARS",
            normalizer.cleanRaw(description),
            normalizedDescription,
            normalizer.cleanRaw(extendedDescription),
            normalizer.cleanRaw(merchantName),
            merchantNormalized,
            normalizer.cleanRaw(counterparty),
            null,
            paymentChannel == null ? MoneyTransaction.PaymentChannel.UNKNOWN : paymentChannel,
            operationKind(signedAmount),
            description,
            null,
            null,
            null,
            null,
            null,
            counterparty,
            null
    ));
  }

  public MoneyTransaction.PaymentChannel inferMercadoPagoPaymentChannel(
          String paymentMethodType,
          String paymentMethod
  ) {
    return paymentChannelDetector.detectMercadoPago("", "", paymentMethodType, paymentMethod, "", "");
  }

  public MoneyTransaction.PaymentChannel inferBancoProvinciaPaymentChannel(
          String description,
          String extendedDescription
  ) {
    return paymentChannelDetector.detectBancoProvincia(description, extendedDescription);
  }

  private ImportClassificationResult ensureExplanation(
          ImportClassificationRule rule,
          NormalizedImportMovement movement,
          ImportClassificationResult result
  ) {
    if (result.explanationJson() != null && !result.explanationJson().isBlank()) {
      return result;
    }

    var matchedField = firstNonBlank(result.matchedField(), rule.fieldName());
    var matchedValue = firstNonBlank(result.matchedValue(), movement.fieldValue(matchedField));

    return ImportClassificationResults.result(
            movement,
            result.classificationLayer() == null ? rule.layer() : result.classificationLayer(),
            matchedField,
            matchedValue,
            result.movementType(),
            result.balanceImpact(),
            result.paymentChannel(),
            result.categorySuggestionKey(),
            result.categorySuggestionName(),
            result.classificationStatus(),
            result.classificationReason(),
            result.confidence(),
            result.rowStatus(),
            result.warning()
    );
  }

  private ImportClassificationResult fallback(NormalizedImportMovement movement) {
    var movementType = movement.amount() != null && movement.amount().signum() < 0
            ? MoneyTransaction.MovementType.EXPENSE
            : MoneyTransaction.MovementType.INCOME;

    return ImportClassificationResults.result(
            movement,
            ClassificationLayer.GENERIC_FALLBACK,
            "normalizedDescription",
            movement.normalizedDescription(),
            movementType,
            MoneyTransaction.BalanceImpact.UNKNOWN,
            movement.paymentChannel(),
            null,
            null,
            MoneyTransaction.ClassificationStatus.NEEDS_CATEGORY,
            "NO_IMPORT_RULE",
            Confidence.LOW,
            RowStatus.NEEDS_CATEGORY,
            "No hay regla confiable para categorizar este movimiento."
    );
  }

  private ImportOperationKind operationKind(BigDecimal signedAmount) {
    if (signedAmount == null || signedAmount.signum() == 0) {
      return ImportOperationKind.UNKNOWN;
    }

    return signedAmount.signum() < 0 ? ImportOperationKind.DEBIT : ImportOperationKind.CREDIT;
  }

  private String normalizedJoin(String... values) {
    if (values == null || values.length == 0) {
      return "";
    }

    var builder = new StringBuilder();

    for (var value : values) {
      var clean = normalizer.cleanRaw(value);

      if (!clean.isBlank()) {
        if (!builder.isEmpty()) {
          builder.append(' ');
        }
        builder.append(clean);
      }
    }

    return builder.toString();
  }

  private String firstNonBlank(String... values) {
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
