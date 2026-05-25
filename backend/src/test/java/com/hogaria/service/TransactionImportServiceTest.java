package com.hogaria.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hogaria.dto.TransactionCreateRequest;
import com.hogaria.dto.TransactionImportDtos.*;
import com.hogaria.entity.*;
import com.hogaria.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
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
  @Mock TransactionService txService;
  @Mock TransactionCategorySuggestionService suggestionService;

  TransactionImportService service;
  UUID userId = UUID.randomUUID();
  UUID profileId = UUID.randomUUID();
  UUID accountId = UUID.randomUUID();

  @BeforeEach
  void setup() {
    service = new TransactionImportService(profileRepository, accountRepository, categoryRepository, txRepository, batchRepository, rowRepository, txService, suggestionService, new ObjectMapper().findAndRegisterModules());
    lenient().when(profileRepository.findByIdAndUserId(eq(profileId), eq(userId))).thenReturn(Optional.of(FinancialProfile.builder().id(profileId).userId(userId).build()));
    lenient().when(accountRepository.existsByIdAndProfileId(eq(accountId), eq(profileId))).thenReturn(true);
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
    when(categoryRepository.save(any())).thenReturn(Category.builder().id(newCatId).profileId(profileId).name("Transporte").type(Category.Type.VARIABLE_EXPENSE).active(true).build());
    when(categoryRepository.findById(newCatId)).thenReturn(Optional.of(Category.builder().id(newCatId).type(Category.Type.VARIABLE_EXPENSE).active(true).build()));
    when(txService.create(any(TransactionCreateRequest.class), eq(userId), any(TransactionService.TransactionMetadata.class))).thenReturn(response(newCatId, MoneyTransaction.MovementType.EXPENSE, "SUBE", new BigDecimal("200")));

    var response = service.commit(userId, profileId, batchId, new TransactionImportCommitRequest(List.of(new TransactionImportCommitRow(1, null, accountId, MoneyTransaction.MovementType.EXPENSE, new BigDecimal("200"), RowStatus.NEEDS_CATEGORY, "SUBE")), true, true));
    assertEquals(1, response.createdCount());
    assertEquals(0, response.failedCount());
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
  void commitCreatesNewCategoryWhenSameNameExistingIsIncompatible() throws Exception {
    UUID batchId = UUID.randomUUID();
    var row = previewRow(1, RowStatus.NEEDS_CATEGORY, "SUELDO", new BigDecimal("200"), null, "Ingresos varios", MoneyTransaction.MovementType.INCOME);
    when(rowRepository.findByBatchIdOrderByRowNumber(batchId)).thenReturn(List.of(toEntity(row)));
    when(categoryRepository.findByProfileIdAndActiveTrue(profileId)).thenReturn(List.of(
            Category.builder().id(UUID.randomUUID()).profileId(profileId).name("Ingresos varios").type(Category.Type.VARIABLE_EXPENSE).active(true).build()
    ));
    when(categoryRepository.findByProfileIdIsNullAndActiveTrue()).thenReturn(List.of());
    UUID newCategoryId = UUID.randomUUID();
    when(categoryRepository.save(any(Category.class))).thenReturn(Category.builder().id(newCategoryId).profileId(profileId).name("Ingresos varios").type(Category.Type.INCOME).active(true).build());
    when(categoryRepository.findById(newCategoryId)).thenReturn(Optional.of(Category.builder().id(newCategoryId).type(Category.Type.INCOME).active(true).build()));
    when(txService.create(any(TransactionCreateRequest.class), eq(userId), any(TransactionService.TransactionMetadata.class))).thenReturn(response(newCategoryId, MoneyTransaction.MovementType.INCOME, "SUELDO", new BigDecimal("200")));

    var response = service.commit(userId, profileId, batchId, new TransactionImportCommitRequest(List.of(new TransactionImportCommitRow(1, null, accountId, MoneyTransaction.MovementType.INCOME, new BigDecimal("200"), RowStatus.NEEDS_CATEGORY, "SUELDO")), true, true));
    assertEquals(1, response.createdCount());
    verify(categoryRepository, times(1)).save(any(Category.class));
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
    return ExcelImportRow.builder().batchId(UUID.randomUUID()).rowNumber(row.rowNumber()).rawJson(new ObjectMapper().findAndRegisterModules().writeValueAsString(row)).build();
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
            amount,
            "ARS",
            description,
            MoneyTransaction.Origin.IMPORT,
            MoneyTransaction.Status.CONFIRMED,
            null,
            null,
            null,
            null,
            null,
            MoneyTransaction.ClassificationStatus.CLASSIFIED,
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

  private Object classify(BigDecimal amount, String detail) throws Exception {
    Method method = TransactionImportService.class.getDeclaredMethod("classifyMercadoPagoMovement", BigDecimal.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class);
    method.setAccessible(true);
    return method.invoke(service, amount, detail, "PAGO APROBADO", "", "", "", "", "", "", "");
  }

  private String movementTypeName(Object classification) throws Exception { return classification.getClass().getDeclaredMethod("movementType").invoke(classification).toString(); }
  private String categoryName(Object classification) throws Exception { return classification.getClass().getDeclaredMethod("categoryName").invoke(classification).toString(); }
  private String statusName(Object classification) throws Exception { return classification.getClass().getDeclaredMethod("status").invoke(classification).toString(); }
}
