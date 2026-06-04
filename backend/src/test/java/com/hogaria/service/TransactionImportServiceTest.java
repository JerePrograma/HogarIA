package com.hogaria.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hogaria.dto.TransactionCreateRequest;
import com.hogaria.dto.TransactionImportDtos.*;
import com.hogaria.entity.*;
import com.hogaria.repository.*;
import com.hogaria.service.transactionimport.DetectedExcelImportFormat;
import com.hogaria.service.transactionimport.ExcelImportTemplate;
import com.hogaria.service.transactionimport.ImportedMovementCandidate;
import com.hogaria.service.transactionimport.TransactionExcelImportFormatDetector;
import com.hogaria.service.transactionimport.TransactionExcelMovementParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionImportServiceTest {
  @Mock FinancialProfileRepository profileRepository;
  @Mock AccountRepository accountRepository;
  @Mock CategoryRepository categoryRepository;
  @Mock MoneyTransactionRepository txRepository;
  @Mock ExcelImportBatchRepository batchRepository;
  @Mock ExcelImportRowRepository rowRepository;
  @Mock TransactionImportReferenceRepository referenceRepository;
  @Mock TransactionClassificationAuditRepository classificationAuditRepository;
  @Mock TransactionService txService;
  @Mock TransactionCategorySuggestionService suggestionService;
  @Mock TransactionExcelImportFormatDetector excelFormatDetector;

  TransactionImportService service;
  UUID userId = UUID.randomUUID();
  UUID profileId = UUID.randomUUID();
  UUID accountId = UUID.randomUUID();

  @BeforeEach
  void setup() {
    service = new TransactionImportService(profileRepository, accountRepository, categoryRepository, txRepository, batchRepository, rowRepository, referenceRepository, classificationAuditRepository, txService, suggestionService, excelFormatDetector, List.<TransactionExcelMovementParser>of(), new ObjectMapper().findAndRegisterModules());
    lenient().when(profileRepository.findByIdAndUserId(eq(profileId), eq(userId))).thenReturn(Optional.of(FinancialProfile.builder().id(profileId).userId(userId).build()));
    lenient().when(accountRepository.existsByIdAndProfileId(eq(accountId), eq(profileId))).thenReturn(true);
    lenient().when(batchRepository.findById(any())).thenReturn(Optional.empty());
  }

  @Test
  void mercadoPagoClassificationsAreCompatible() throws Exception {
    var cj = classify(new BigDecimal("100"), "LINK DE PAGO PRESTAMOS");
    assertEquals("SAVING", movementTypeName(cj));
    assertEquals("CJ - Capital recuperado", categoryName(cj));

    var credito = classify(new BigDecimal("100"), "PAYMENT LINKED TO A LOAN ORIGINATION");
    assertEquals("ADJUSTMENT", movementTypeName(credito));
    assertEquals("Créditos y financiación", categoryName(credito));

    var sube = classify(new BigDecimal("-2000"), "RECARGA SUBE");
    assertEquals("EXPENSE", movementTypeName(sube));

    var ruido = classify(new BigDecimal("1"), "");
    assertEquals("SKIPPED", statusName(ruido));
  }

  @Test
  void commitCreatesMissingCategoryWhenEnabled() throws Exception {
    UUID batchId = UUID.randomUUID();
    var row = previewRow(1, RowStatus.NEEDS_CATEGORY, "SUBE", new BigDecimal("200"), null, "Transporte", MoneyTransaction.MovementType.EXPENSE);
    when(rowRepository.findByBatchIdOrderByRowNumber(batchId)).thenReturn(List.of(toEntity(row)));
    when(categoryRepository.findByProfileIdAndActiveTrue(profileId)).thenReturn(List.of());
    when(categoryRepository.findByProfileIdIsNullAndActiveTrue()).thenReturn(List.of());
    UUID newCatId = UUID.randomUUID();
    when(categoryRepository.save(any())).thenReturn(Category.builder().id(newCatId).profileId(profileId).name("Otros a revisar").type(Category.Type.VARIABLE_EXPENSE).active(true).build());
    when(categoryRepository.findById(newCatId)).thenReturn(Optional.of(Category.builder().id(newCatId).type(Category.Type.VARIABLE_EXPENSE).active(true).build()));
    when(txService.create(any(TransactionCreateRequest.class), eq(userId), any(TransactionService.TransactionMetadata.class))).thenReturn(response(newCatId, MoneyTransaction.MovementType.EXPENSE, "SUBE", new BigDecimal("200")));

    var response = service.commit(userId, profileId, batchId, new TransactionImportCommitRequest(List.of(new TransactionImportCommitRow(1, null, accountId, MoneyTransaction.MovementType.EXPENSE, new BigDecimal("200"), RowStatus.NEEDS_CATEGORY, "SUBE")), true, true));
    assertEquals(1, response.createdCount());
    assertEquals(0, response.failedCount());
    var categoryCaptor = ArgumentCaptor.forClass(Category.class);
    verify(categoryRepository).save(categoryCaptor.capture());
    assertEquals("Otros a revisar", categoryCaptor.getValue().getName());
  }

  @Test
  void commitFailsNeedsCategoryWhenFallbackDisabled() throws Exception {
    UUID batchId = UUID.randomUUID();
    var row = previewRow(1, RowStatus.NEEDS_CATEGORY, "SUBE", new BigDecimal("200"), null, "Transporte", MoneyTransaction.MovementType.EXPENSE);
    when(rowRepository.findByBatchIdOrderByRowNumber(batchId)).thenReturn(List.of(toEntity(row)));

    var response = service.commit(userId, profileId, batchId, new TransactionImportCommitRequest(List.of(new TransactionImportCommitRow(1, null, accountId, MoneyTransaction.MovementType.EXPENSE, new BigDecimal("200"), RowStatus.NEEDS_CATEGORY, "SUBE")), false, true));
    assertEquals(0, response.createdCount());
    assertEquals(1, response.failedCount());
  }

  @Test
  void commitImportsGenericReviewIncomeWithoutCategoryAsPendingReview() throws Exception {
    UUID batchId = UUID.randomUUID();
    var row = previewRow(
            1,
            RowStatus.REVIEW,
            "Varios",
            new BigDecimal("5000"),
            null,
            null,
            MoneyTransaction.MovementType.INCOME,
            MoneyTransaction.BalanceImpact.UNKNOWN,
            MoneyTransaction.ClassificationStatus.REVIEW,
            "RULE_MP_GENERIC_INFLOW_REVIEW"
    );
    when(rowRepository.findByBatchIdOrderByRowNumber(batchId)).thenReturn(List.of(toEntity(row)));
    when(txService.create(any(TransactionCreateRequest.class), eq(userId), any(TransactionService.TransactionMetadata.class)))
            .thenReturn(response(null, MoneyTransaction.MovementType.INCOME, "Varios", new BigDecimal("5000")));

    var response = service.commit(userId, profileId, batchId, new TransactionImportCommitRequest(List.of(
            new TransactionImportCommitRow(1, null, accountId, MoneyTransaction.MovementType.INCOME, new BigDecimal("5000"), RowStatus.REVIEW, "Varios")
    ), true, true));

    assertEquals(1, response.createdCount());
    assertEquals(0, response.failedCount());
    var requestCaptor = ArgumentCaptor.forClass(TransactionCreateRequest.class);
    var metadataCaptor = ArgumentCaptor.forClass(TransactionService.TransactionMetadata.class);
    verify(txService).create(requestCaptor.capture(), eq(userId), metadataCaptor.capture());
    assertNull(requestCaptor.getValue().categoryId());
    assertEquals(MoneyTransaction.Status.PENDING, requestCaptor.getValue().status());
    assertEquals(MoneyTransaction.ClassificationStatus.REVIEW, requestCaptor.getValue().classificationStatus());
    assertEquals("RULE_MP_GENERIC_INFLOW_REVIEW", requestCaptor.getValue().classificationReason());
    assertEquals(MoneyTransaction.ClassificationStatus.REVIEW, metadataCaptor.getValue().classificationStatus());
    assertEquals("RULE_MP_GENERIC_INFLOW_REVIEW", metadataCaptor.getValue().classificationReason());
    verify(categoryRepository, never()).save(any());
  }

  @Test
  void commitImportsTechnicalInternalTransferWithoutCategoryAsConfirmedNeutralMovement() throws Exception {
    UUID batchId = UUID.randomUUID();
    var row = previewRow(
            1,
            RowStatus.REVIEW,
            "Pago Debin | Bank Transfer",
            new BigDecimal("400000"),
            null,
            "Fondeo MercadoPago / transferencias internas",
            MoneyTransaction.MovementType.TRANSFER,
            MoneyTransaction.BalanceImpact.INTERNAL_TRANSFER,
            MoneyTransaction.ClassificationStatus.TECHNICAL,
            "RULE_MP_FUNDING_TRANSFER"
    );
    when(rowRepository.findByBatchIdOrderByRowNumber(batchId)).thenReturn(List.of(toEntity(row)));
    when(txService.create(any(TransactionCreateRequest.class), eq(userId), any(TransactionService.TransactionMetadata.class)))
            .thenReturn(response(null, MoneyTransaction.MovementType.TRANSFER, "Pago Debin | Bank Transfer", new BigDecimal("400000")));

    var response = service.commit(userId, profileId, batchId, new TransactionImportCommitRequest(List.of(
            new TransactionImportCommitRow(1, null, accountId, MoneyTransaction.MovementType.TRANSFER, new BigDecimal("400000"), RowStatus.REVIEW, "Pago Debin | Bank Transfer")
    ), false, true));

    assertEquals(1, response.createdCount());
    assertEquals(0, response.failedCount());
    var requestCaptor = ArgumentCaptor.forClass(TransactionCreateRequest.class);
    var metadataCaptor = ArgumentCaptor.forClass(TransactionService.TransactionMetadata.class);
    verify(txService).create(requestCaptor.capture(), eq(userId), metadataCaptor.capture());
    assertNull(requestCaptor.getValue().categoryId());
    assertEquals(MoneyTransaction.MovementType.TRANSFER, requestCaptor.getValue().movementType());
    assertEquals(MoneyTransaction.Status.CONFIRMED, requestCaptor.getValue().status());
    assertEquals(MoneyTransaction.ClassificationStatus.TECHNICAL, requestCaptor.getValue().classificationStatus());
    assertEquals("RULE_MP_FUNDING_TRANSFER", requestCaptor.getValue().classificationReason());
    assertEquals(MoneyTransaction.BalanceImpact.INTERNAL_TRANSFER, metadataCaptor.getValue().balanceImpact());
  }

  @Test
  void commitImportsNeutralMercadoCreditoAdjustmentWithoutCategoryAsReviewNeutral() throws Exception {
    UUID batchId = UUID.randomUUID();
    var row = previewRow(
            1,
            RowStatus.REVIEW,
            "Payment linked to a loan origination",
            new BigDecimal("120000"),
            null,
            "Mercado Crédito",
            MoneyTransaction.MovementType.ADJUSTMENT,
            MoneyTransaction.BalanceImpact.NEUTRAL_ADJUSTMENT,
            MoneyTransaction.ClassificationStatus.REVIEW,
            "RULE_MERCADO_CREDITO_LOAN_ORIGINATION_REVIEW"
    );
    when(rowRepository.findByBatchIdOrderByRowNumber(batchId)).thenReturn(List.of(toEntity(row)));
    when(txService.create(any(TransactionCreateRequest.class), eq(userId), any(TransactionService.TransactionMetadata.class)))
            .thenReturn(response(null, MoneyTransaction.MovementType.ADJUSTMENT, "Payment linked to a loan origination", new BigDecimal("120000")));

    var response = service.commit(userId, profileId, batchId, new TransactionImportCommitRequest(List.of(
            new TransactionImportCommitRow(1, null, accountId, MoneyTransaction.MovementType.ADJUSTMENT, new BigDecimal("120000"), RowStatus.REVIEW, "Payment linked to a loan origination")
    ), false, true));

    assertEquals(1, response.createdCount());
    assertEquals(0, response.failedCount());
    var requestCaptor = ArgumentCaptor.forClass(TransactionCreateRequest.class);
    var metadataCaptor = ArgumentCaptor.forClass(TransactionService.TransactionMetadata.class);
    verify(txService).create(requestCaptor.capture(), eq(userId), metadataCaptor.capture());
    assertNull(requestCaptor.getValue().categoryId());
    assertEquals(MoneyTransaction.MovementType.ADJUSTMENT, requestCaptor.getValue().movementType());
    assertEquals(MoneyTransaction.Status.CONFIRMED, requestCaptor.getValue().status());
    assertEquals(MoneyTransaction.ClassificationStatus.REVIEW, requestCaptor.getValue().classificationStatus());
    assertEquals("RULE_MERCADO_CREDITO_LOAN_ORIGINATION_REVIEW", requestCaptor.getValue().classificationReason());
    assertEquals(MoneyTransaction.BalanceImpact.NEUTRAL_ADJUSTMENT, metadataCaptor.getValue().balanceImpact());
  }

  @Test
  void commitStillFailsWhenSelectedCategoryIsIncompatibleWithMovementType() throws Exception {
    UUID batchId = UUID.randomUUID();
    UUID categoryId = UUID.randomUUID();
    var row = previewRow(1, RowStatus.READY, "Varios", new BigDecimal("5000"), categoryId, "Sueldo", MoneyTransaction.MovementType.EXPENSE);
    when(rowRepository.findByBatchIdOrderByRowNumber(batchId)).thenReturn(List.of(toEntity(row)));
    when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(Category.builder().id(categoryId).type(Category.Type.INCOME).active(true).build()));

    var response = service.commit(userId, profileId, batchId, new TransactionImportCommitRequest(List.of(
            new TransactionImportCommitRow(1, categoryId, accountId, MoneyTransaction.MovementType.EXPENSE, new BigDecimal("5000"), RowStatus.READY, "Varios")
    ), false, true));

    assertEquals(0, response.createdCount());
    assertEquals(1, response.failedCount());
    verify(txService, never()).create(any(), any(), any());
  }

  @Test
  void commitMixedStatusesAggregatesAllCounters() throws Exception {
    UUID batchId = UUID.randomUUID();
    var rows = List.of(
            previewRow(1, RowStatus.READY, "ok", new BigDecimal("100"), UUID.randomUUID(), "Cat", MoneyTransaction.MovementType.EXPENSE),
            previewRow(2, RowStatus.NEEDS_CATEGORY, "nc", new BigDecimal("100"), null, "NoCat", MoneyTransaction.MovementType.EXPENSE),
            previewRow(3, RowStatus.SKIPPED, "skip", new BigDecimal("100"), null, null, MoneyTransaction.MovementType.EXPENSE),
            previewRow(4, RowStatus.DUPLICATE, "dup", new BigDecimal("100"), UUID.randomUUID(), "Cat", MoneyTransaction.MovementType.EXPENSE),
            previewRow(5, RowStatus.ERROR, "err", new BigDecimal("100"), null, null, MoneyTransaction.MovementType.EXPENSE)
    );
    when(rowRepository.findByBatchIdOrderByRowNumber(batchId)).thenReturn(rows.stream().map(r -> {
      try { return toEntity(r); } catch (Exception e) { throw new RuntimeException(e); }
    }).toList());
    when(categoryRepository.findById(any())).thenReturn(Optional.of(Category.builder().id(UUID.randomUUID()).type(Category.Type.VARIABLE_EXPENSE).active(true).build()));
    when(txService.create(any(TransactionCreateRequest.class), eq(userId), any(TransactionService.TransactionMetadata.class))).thenReturn(response(UUID.randomUUID(), MoneyTransaction.MovementType.EXPENSE, "ok", new BigDecimal("100")));

    var commitRows = List.of(
            new TransactionImportCommitRow(1, rows.get(0).suggestedCategoryId(), accountId, MoneyTransaction.MovementType.EXPENSE, new BigDecimal("100"), RowStatus.READY, "ok"),
            new TransactionImportCommitRow(2, null, accountId, MoneyTransaction.MovementType.EXPENSE, new BigDecimal("100"), RowStatus.NEEDS_CATEGORY, "nc"),
            new TransactionImportCommitRow(3, null, accountId, MoneyTransaction.MovementType.EXPENSE, new BigDecimal("100"), RowStatus.SKIPPED, "skip"),
            new TransactionImportCommitRow(4, rows.get(3).suggestedCategoryId(), accountId, MoneyTransaction.MovementType.EXPENSE, new BigDecimal("100"), RowStatus.DUPLICATE, "dup"),
            new TransactionImportCommitRow(5, null, accountId, MoneyTransaction.MovementType.EXPENSE, new BigDecimal("100"), RowStatus.ERROR, "err")
    );

    var response = service.commit(userId, profileId, batchId, new TransactionImportCommitRequest(commitRows, false, true));
    assertEquals(1, response.createdCount());
    assertEquals(1, response.skippedCount());
    assertEquals(1, response.duplicateCount());
    assertEquals(2, response.failedCount());
  }

  @Test
  void commitFallbackUsesReviewCategoryEvenWhenSuggestedNameExists() throws Exception {
    UUID batchId = UUID.randomUUID();
    var row = previewRow(1, RowStatus.NEEDS_CATEGORY, "SUELDO", new BigDecimal("200"), null, "Ingresos varios", MoneyTransaction.MovementType.INCOME);
    when(rowRepository.findByBatchIdOrderByRowNumber(batchId)).thenReturn(List.of(toEntity(row)));
    when(categoryRepository.findByProfileIdAndActiveTrue(profileId)).thenReturn(List.of(
            Category.builder().id(UUID.randomUUID()).profileId(profileId).name("Ingresos varios").type(Category.Type.VARIABLE_EXPENSE).active(true).build()
    ));
    when(categoryRepository.findByProfileIdIsNullAndActiveTrue()).thenReturn(List.of());
    UUID newCategoryId = UUID.randomUUID();
    when(categoryRepository.save(any(Category.class))).thenReturn(Category.builder().id(newCategoryId).profileId(profileId).name("Otros a revisar").type(Category.Type.INCOME).active(true).build());
    when(categoryRepository.findById(newCategoryId)).thenReturn(Optional.of(Category.builder().id(newCategoryId).type(Category.Type.INCOME).active(true).build()));
    when(txService.create(any(TransactionCreateRequest.class), eq(userId), any(TransactionService.TransactionMetadata.class))).thenReturn(response(newCategoryId, MoneyTransaction.MovementType.INCOME, "SUELDO", new BigDecimal("200")));

    var response = service.commit(userId, profileId, batchId, new TransactionImportCommitRequest(List.of(new TransactionImportCommitRow(1, null, accountId, MoneyTransaction.MovementType.INCOME, new BigDecimal("200"), RowStatus.NEEDS_CATEGORY, "SUELDO")), true, true));
    assertEquals(1, response.createdCount());
    var categoryCaptor = ArgumentCaptor.forClass(Category.class);
    verify(categoryRepository, times(1)).save(categoryCaptor.capture());
    assertEquals("Otros a revisar", categoryCaptor.getValue().getName());
    assertEquals(Category.Type.INCOME, categoryCaptor.getValue().getType());
  }

  @Test
  void previewDoesNotCreateMoneyTransactions() {
    var parser = mock(TransactionExcelMovementParser.class);
    service = new TransactionImportService(profileRepository, accountRepository, categoryRepository, txRepository, batchRepository, rowRepository, referenceRepository, classificationAuditRepository, txService, suggestionService, excelFormatDetector, List.of(parser), new ObjectMapper().findAndRegisterModules());
    var batchId = UUID.randomUUID();
    var categoryId = UUID.randomUUID();
    var detection = new DetectedExcelImportFormat(
            ExcelImportTemplate.MERCADO_PAGO_SETTLEMENT,
            "sheet0",
            0,
            List.of("FECHA DE ORIGEN", "MONTO NETO DE LA OPERACIÓN QUE IMPACTÓ TU DINERO"),
            Map.of()
    );
    var candidate = ImportedMovementCandidate.builder()
            .source(TransactionImportSource.MERCADO_PAGO)
            .detectedFormat("MERCADO_PAGO_SETTLEMENT")
            .sourceOperationId("op-1")
            .sourceHash("hash")
            .realDate(LocalDate.of(2026, 5, 1))
            .budgetDate(LocalDate.of(2026, 5, 1))
            .operationDateTime(LocalDate.of(2026, 5, 1).atStartOfDay())
            .operationDateTimePrecision(MoneyTransaction.OperationDateTimePrecision.DATE_TIME)
            .signedAmount(new BigDecimal("-100"))
            .amountAbs(new BigDecimal("100"))
            .currency("ARS")
            .rawDescription("Uber")
            .normalizedDescription("uber")
            .paymentChannel(MoneyTransaction.PaymentChannel.MERCADO_PAGO)
            .movementType(MoneyTransaction.MovementType.EXPENSE)
            .balanceImpact(MoneyTransaction.BalanceImpact.CONSUMPTION_EXPENSE)
            .categorySuggestionKey("taxiyapps")
            .categorySuggestionName("Taxi y apps")
            .classificationStatus(MoneyTransaction.ClassificationStatus.CLASSIFIED)
            .classificationReason("RULE_UBER")
            .confidence(Confidence.HIGH)
            .rawJson("{\"DETALLE DE LA VENTA\":\"Uber\"}")
            .rowNumber(2)
            .sheetName("sheet0")
            .targetEntity(ImportTargetEntity.EXPENSE)
            .rowStatus(RowStatus.READY)
            .build();
    var file = new MockMultipartFile("file", "mp.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{'P', 'K'});
    when(excelFormatDetector.detect(any())).thenReturn(detection);
    when(parser.template()).thenReturn(ExcelImportTemplate.MERCADO_PAGO_SETTLEMENT);
    when(parser.parse(any(), eq(detection), eq(profileId), eq(accountId))).thenReturn(List.of(candidate));
    when(categoryRepository.findByProfileIdAndActiveTrue(profileId)).thenReturn(List.of());
    when(categoryRepository.findByProfileIdIsNullAndActiveTrue()).thenReturn(List.of(
            Category.builder().id(categoryId).name("Taxi y apps").categoryKey("taxiyapps").type(Category.Type.VARIABLE_EXPENSE).active(true).build()
    ));
    when(batchRepository.save(any())).thenAnswer(invocation -> {
      ExcelImportBatch batch = invocation.getArgument(0);
      batch.setId(batchId);
      return batch;
    });
    when(rowRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var preview = service.preview(userId, profileId, accountId, TransactionImportSource.AUTO, file, null, null);

    assertEquals(batchId, preview.batchId());
    assertEquals("MERCADO_PAGO_SETTLEMENT", preview.detectedFormat());
    assertEquals(1, preview.importableRows());
    verify(txService, never()).create(any(), any(), any());
  }

  @Test
  void commitCreatesTransactionImportReference() throws Exception {
    UUID batchId = UUID.randomUUID();
    UUID categoryId = UUID.randomUUID();
    var row = new TransactionImportPreviewRow(
            3,
            TransactionImportSource.BANCO_PROVINCIA,
            "9332",
            "abc123hash",
            LocalDate.of(2026, 1, 30),
            LocalDate.of(2026, 1, 30),
            "PAGO CON TARJETA DEBITO",
            "pago con tarjeta debito payu*ar*uber",
            new BigDecimal("-5378"),
            new BigDecimal("5378"),
            "ARS",
            MoneyTransaction.MovementType.EXPENSE,
            categoryId,
            "Taxi y apps",
            Confidence.HIGH,
            RowStatus.READY,
            null,
            "{}",
            null,
            null,
            null,
            null,
            null,
            null,
            "BANCO_PROVINCIA_MOVIMIENTOS",
            LocalDate.of(2026, 1, 30).atStartOfDay(),
            MoneyTransaction.OperationDateTimePrecision.DATE_ONLY,
            "COMPRA TARJETA 29/01/26 23:43",
            "PAYU*AR*UBER",
            "PAYU*AR*UBER",
            null,
            MoneyTransaction.PaymentChannel.DEBIT_CARD,
            MoneyTransaction.BalanceImpact.CONSUMPTION_EXPENSE,
            MoneyTransaction.ClassificationStatus.CLASSIFIED,
            "RULE_UBER",
            "MERCHANT_ALIAS",
            "merchant",
            "PAYU*AR*UBER",
            "{\"reasonCode\":\"RULE_UBER\"}",
            "taxiyapps",
            "9332",
            "Hoja 1",
            ImportTargetEntity.EXPENSE,
            "{\"Número Secuencia\":\"9332\"}"
    );
    when(rowRepository.findByBatchIdOrderByRowNumber(batchId)).thenReturn(List.of(toEntity(row)));
    when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(Category.builder().id(categoryId).type(Category.Type.VARIABLE_EXPENSE).active(true).build()));
    when(txService.create(any(TransactionCreateRequest.class), eq(userId), any(TransactionService.TransactionMetadata.class)))
            .thenReturn(response(categoryId, MoneyTransaction.MovementType.EXPENSE, "PAGO CON TARJETA DEBITO", new BigDecimal("5378")));

    var response = service.commit(userId, profileId, batchId, new TransactionImportCommitRequest(List.of(
            new TransactionImportCommitRow(3, categoryId, accountId, MoneyTransaction.MovementType.EXPENSE, new BigDecimal("5378"), RowStatus.READY, "PAGO CON TARJETA DEBITO")
    ), false, true));

    assertEquals(1, response.createdCount());
    var captor = ArgumentCaptor.forClass(TransactionImportReference.class);
    verify(referenceRepository).save(captor.capture());
    assertEquals(profileId, captor.getValue().getProfileId());
    assertEquals(accountId, captor.getValue().getAccountId());
    assertEquals("BANCO_PROVINCIA", captor.getValue().getImportSource());
    assertEquals("9332", captor.getValue().getSourceOperationId());
    assertEquals("abc123hash", captor.getValue().getSourceHash());
    assertEquals("9332", captor.getValue().getExternalSequence());
    assertEquals("PAYU*AR*UBER", captor.getValue().getMerchantName());
  }

  @Test
  void bancoDebinAndMercadoPagoPagoDebinSameAmountNearDateIsInternalTransfer() throws Exception {
    var date = LocalDate.of(2026, 5, 7);
    var amount = new BigDecimal("400000.00");
    var mpAccountId = UUID.randomUUID();
    var mpTxId = UUID.randomUUID();
    var mpFunding = MoneyTransaction.builder()
            .id(mpTxId)
            .profileId(profileId)
            .accountId(mpAccountId)
            .source("MERCADO_PAGO")
            .description("Pago Debin | Bank Transfer")
            .movementType(MoneyTransaction.MovementType.TRANSFER)
            .paymentChannel(MoneyTransaction.PaymentChannel.MERCADO_PAGO)
            .realDate(date.minusDays(1))
            .amount(amount)
            .build();
    when(txRepository.findByProfileIdAndRealDateBetweenAndAmount(profileId, date.minusDays(2), date.plusDays(2), amount))
            .thenReturn(List.of(mpFunding));
    when(txRepository.findById(mpTxId)).thenReturn(Optional.of(mpFunding));

    var row = previewRow(1, TransactionImportSource.BANCO_PROVINCIA, RowStatus.READY, "DEBITO DEBIN", amount, UUID.randomUUID(), "Cuenta DNI / DEBIN", MoneyTransaction.MovementType.EXPENSE, date);

    var rows = applyDuplicateStatus(row);

    assertEquals(RowStatus.INTERNAL_TRANSFER_MATCHED, rows.get(0).status());
    assertEquals(MoneyTransaction.MovementType.TRANSFER, rows.get(0).movementType());
    assertEquals("INTERNAL_TRANSFER_MATCHED", rows.get(0).matchType());
  }

  @Test
  void mercadoPagoDebitCardAndBancoDebitCardSameAmountNearDateIsCrossSourceDuplicate() throws Exception {
    var date = LocalDate.of(2026, 5, 14);
    var amount = new BigDecimal("56654.12");
    var bpTxId = UUID.randomUUID();
    var bpPurchase = MoneyTransaction.builder()
            .id(bpTxId)
            .profileId(profileId)
            .accountId(accountId)
            .source("BANCO_PROVINCIA")
            .description("PAGO CON TARJETA DEBITO")
            .movementType(MoneyTransaction.MovementType.EXPENSE)
            .realDate(date.plusDays(1))
            .amount(amount)
            .build();
    when(txRepository.findByProfileIdAndRealDateBetweenAndAmount(profileId, date.minusDays(2), date.plusDays(2), amount))
            .thenReturn(List.of(bpPurchase));
    when(txRepository.findById(bpTxId)).thenReturn(Optional.of(bpPurchase));

    var row = previewRow(1, TransactionImportSource.MERCADO_PAGO, RowStatus.READY, "Compra final | Tarjeta de débito Visa", amount, UUID.randomUUID(), "Compras con tarjeta", MoneyTransaction.MovementType.EXPENSE, date);

    var rows = applyDuplicateStatus(row);

    assertEquals(RowStatus.POSSIBLE_CROSS_SOURCE_DUPLICATE, rows.get(0).status());
    assertEquals("POSSIBLE_CROSS_SOURCE_DUPLICATE", rows.get(0).matchType());
  }

  private ExcelImportRow toEntity(TransactionImportPreviewRow row) throws Exception {
    return ExcelImportRow.builder().id(UUID.randomUUID()).batchId(UUID.randomUUID()).rowNumber(row.rowNumber()).rawJson(new ObjectMapper().findAndRegisterModules().writeValueAsString(row)).build();
  }

  @SuppressWarnings("unchecked")
  private List<TransactionImportPreviewRow> applyDuplicateStatus(TransactionImportPreviewRow row) throws Exception {
    Method method = TransactionImportService.class.getDeclaredMethod("applyDuplicateStatus", UUID.class, UUID.class, List.class);
    method.setAccessible(true);
    return (List<TransactionImportPreviewRow>) method.invoke(service, profileId, accountId, List.of(row));
  }

  private com.hogaria.dto.TransactionResponse response(UUID categoryId, MoneyTransaction.MovementType movementType, String description, BigDecimal amount) {
    return new com.hogaria.dto.TransactionResponse(
            UUID.randomUUID(),
            profileId,
            accountId,
            categoryId,
            movementType,
            LocalDate.now(),
            LocalDate.now(),
            null,
            MoneyTransaction.OperationDateTimePrecision.DATE_ONLY,
            amount,
            "ARS",
            description,
            description,
            MoneyTransaction.Origin.IMPORT,
            MoneyTransaction.Status.CONFIRMED,
            null,
            null,
            null,
            null,
            MoneyTransaction.BalanceImpact.UNKNOWN,
            null,
            null,
            MoneyTransaction.ClassificationStatus.CLASSIFIED,
            null,
            null,
            null,
            null,
            null,
            null
    );
  }

  private TransactionImportPreviewRow previewRow(int n, RowStatus status, String desc, BigDecimal amount, UUID catId, String catName, MoneyTransaction.MovementType mt) {
    return previewRow(n, TransactionImportSource.MERCADO_PAGO, status, desc, amount, catId, catName, mt, LocalDate.now());
  }

  private TransactionImportPreviewRow previewRow(int n, TransactionImportSource source, RowStatus status, String desc, BigDecimal amount, UUID catId, String catName, MoneyTransaction.MovementType mt, LocalDate date) {
    return new TransactionImportPreviewRow(n, source, null, null, date, date, desc, desc, amount, amount, "ARS", mt, catId, catName, Confidence.HIGH, status, "", "{}", null, null, null, null, null, null);
  }

  private TransactionImportPreviewRow previewRow(
          int n,
          RowStatus status,
          String desc,
          BigDecimal amount,
          UUID catId,
          String catName,
          MoneyTransaction.MovementType movementType,
          MoneyTransaction.BalanceImpact balanceImpact,
          MoneyTransaction.ClassificationStatus classificationStatus,
          String classificationReason
  ) {
    var date = LocalDate.now();
    return new TransactionImportPreviewRow(
            n,
            TransactionImportSource.MERCADO_PAGO,
            null,
            null,
            date,
            date,
            desc,
            desc,
            amount,
            amount,
            "ARS",
            movementType,
            catId,
            catName,
            Confidence.LOW,
            status,
            "",
            "{}",
            null,
            null,
            null,
            null,
            null,
            null,
            "MERCADO_PAGO_SETTLEMENT",
            date.atStartOfDay(),
            MoneyTransaction.OperationDateTimePrecision.DATE_ONLY,
            desc,
            null,
            null,
            null,
            MoneyTransaction.PaymentChannel.MERCADO_PAGO,
            balanceImpact,
            classificationStatus,
            classificationReason,
            null,
            null,
            null,
            null,
            null,
            null,
            "Hoja 1",
            ImportTargetEntity.EXPENSE,
            "{}"
    );
  }

  private Object classify(BigDecimal amount, String detail) throws Exception {
    Method method = TransactionImportService.class.getDeclaredMethod("classifyMercadoPagoMovement", BigDecimal.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class);
    method.setAccessible(true);
    return method.invoke(service, amount, detail, "PAGO APROBADO", "", "", "", "", "", "", "");
  }

  private String movementTypeName(Object classification) throws Exception { return classification.getClass().getDeclaredMethod("movementType").invoke(classification).toString(); }
  private String categoryName(Object classification) throws Exception { return classification.getClass().getDeclaredMethod("categoryName").invoke(classification).toString(); }
  private String statusName(Object classification) throws Exception { return classification.getClass().getDeclaredMethod("status").invoke(classification).toString(); }
}
