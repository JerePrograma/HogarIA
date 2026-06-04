package com.hogaria.service.transactionimport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hogaria.dto.TransactionImportDtos.Confidence;
import com.hogaria.entity.MoneyTransaction;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransactionImportRuleClassifierTest {

  private ImportTextNormalizer normalizer;
  private TransactionImportRuleClassifier classifier;

  @BeforeEach
  void setup() {
    normalizer = new ImportTextNormalizer();
    classifier = new TransactionImportRuleClassifier(normalizer);
  }

  @Test
  void classifiesBancoProvinciaCasesFromSanitizedFixture() throws Exception {
    var detector = new PaymentChannelDetector(normalizer);

    for (var fixture : load("banco-provincia-classification-cases.json", new TypeReference<List<BancoCase>>() {})) {
      var result = classifier.classifyBancoProvincia(
              new BigDecimal(fixture.amount()),
              fixture.description(),
              fixture.extendedDescription(),
              fixture.merchant(),
              detector.detectBancoProvincia(fixture.description(), fixture.extendedDescription()),
              null
      );

      assertResult(fixture.name(), fixture.expectedCategoryKey(), fixture.expectedStatus(), fixture.expectedConfidence(), fixture.expectedReason(), result);
    }
  }

  @Test
  void classifiesMercadoPagoCasesFromSanitizedFixture() throws Exception {
    var detector = new PaymentChannelDetector(normalizer);

    for (var fixture : load("mercado-pago-classification-cases.json", new TypeReference<List<MercadoPagoCase>>() {})) {
      var result = classifier.classifyMercadoPago(
              new BigDecimal(fixture.amount()),
              fixture.detail(),
              fixture.operationType(),
              fixture.paymentMethodType(),
              fixture.paymentMethod(),
              fixture.payer(),
              fixture.liquidated(),
              fixture.operationTags(),
              fixture.identificationNumber(),
              detector.detectMercadoPago(
                      fixture.detail(),
                      fixture.operationType(),
                      fixture.paymentMethodType(),
                      fixture.paymentMethod(),
                      fixture.identificationNumber(),
                      fixture.operationTags()
              )
      );

      assertResult(fixture.name(), fixture.expectedCategoryKey(), fixture.expectedStatus(), fixture.expectedConfidence(), fixture.expectedReason(), result);
    }
  }

  @Test
  void debitCardFallbackDoesNotHideSpecificMerchantRules() {
    var result = classifier.classifyBancoProvincia(
            new BigDecimal("-2100.00"),
            "PAGO CON TARJETA DEBITO",
            "COMPRA TARJETA SANITIZADA",
            "DIA TIENDA 3",
            MoneyTransaction.PaymentChannel.DEBIT_CARD,
            null
    );

    assertEquals("RULE_MERCHANT_DIA", result.classificationReason());
    assertEquals("supermercado", result.categorySuggestionKey());
    assertEquals(ClassificationLayer.MERCHANT_ALIAS, result.classificationLayer());
  }

  @Test
  void directDebitFallbackDoesNotHideArcaMonotributoRule() {
    var result = classifier.classifyBancoProvincia(
            new BigDecimal("-34000.00"),
            "DEBITO PAGO DIRECTO",
            "ARCA (EX AFIP) RE.MONOTR02/26",
            "",
            MoneyTransaction.PaymentChannel.DIRECT_DEBIT,
            null
    );

    assertEquals("RULE_ARCA_AFIP_MONOTRIBUTO", result.classificationReason());
    assertEquals("monotributo", result.categorySuggestionKey());
    assertEquals(ClassificationLayer.SOURCE_SPECIFIC, result.classificationLayer());
  }

  private void assertResult(
          String name,
          String expectedCategoryKey,
          String expectedStatus,
          String expectedConfidence,
          String expectedReason,
          ImportClassificationResult result
  ) {
    assertEquals(expectedCategoryKey, result.categorySuggestionKey(), name + " category");
    assertEquals(MoneyTransaction.ClassificationStatus.valueOf(expectedStatus), result.classificationStatus(), name + " status");
    assertEquals(Confidence.valueOf(expectedConfidence), result.confidence(), name + " confidence");
    assertEquals(expectedReason, result.classificationReason(), name + " reason");
    assertNotNull(result.explanationJson(), name + " explanation");
  }

  private <T> T load(String fileName, TypeReference<T> type) throws Exception {
    var mapper = new ObjectMapper();
    try (var stream = getClass().getResourceAsStream("/fixtures/transaction-import/" + fileName)) {
      assertNotNull(stream, fileName);
      return mapper.readValue(stream, type);
    }
  }

  private record BancoCase(
          String name,
          String amount,
          String description,
          String extendedDescription,
          String merchant,
          String expectedCategoryKey,
          String expectedStatus,
          String expectedConfidence,
          String expectedReason
  ) {
  }

  private record MercadoPagoCase(
          String name,
          String amount,
          String detail,
          String operationType,
          String paymentMethodType,
          String paymentMethod,
          String liquidated,
          String operationTags,
          String identificationNumber,
          String payer,
          String expectedCategoryKey,
          String expectedStatus,
          String expectedConfidence,
          String expectedReason
  ) {
  }
}
