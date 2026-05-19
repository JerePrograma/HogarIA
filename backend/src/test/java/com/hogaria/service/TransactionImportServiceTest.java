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

  TransactionImportService service;
  UUID userId = UUID.randomUUID();
  UUID profileId = UUID.randomUUID();
  UUID accountId = UUID.randomUUID();

  @BeforeEach
  void setup() {
    service = new TransactionImportService(profileRepository, accountRepository, categoryRepository, txRepository, batchRepository, rowRepository, txService, new ObjectMapper().findAndRegisterModules());
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
    when(txService.create(any(TransactionCreateRequest.class), eq(userId))).thenReturn(new com.hogaria.dto.TransactionResponse(UUID.randomUUID(), profileId, accountId, newCatId, MoneyTransaction.MovementType.EXPENSE, LocalDate.now(), LocalDate.now(), new BigDecimal("200"), "ARS", "SUBE", MoneyTransaction.Origin.IMPORT, MoneyTransaction.Status.CONFIRMED, null, null));

    var response = service.commit(userId, profileId, batchId, new TransactionImportCommitRequest(List.of(new TransactionImportCommitRow(1, null, accountId, MoneyTransaction.MovementType.EXPENSE, new BigDecimal("200"), RowStatus.NEEDS_CATEGORY, "SUBE")), true, true));
    assertEquals(1, response.createdCount());
    assertEquals(0, response.failedCount());
  }

  private ExcelImportRow toEntity(TransactionImportPreviewRow row) throws Exception {
    return ExcelImportRow.builder().batchId(UUID.randomUUID()).rowNumber(row.rowNumber()).rawJson(new ObjectMapper().findAndRegisterModules().writeValueAsString(row)).build();
  }

  private TransactionImportPreviewRow previewRow(int n, RowStatus status, String desc, BigDecimal amount, UUID catId, String catName, MoneyTransaction.MovementType mt) {
    return new TransactionImportPreviewRow(n, TransactionImportSource.MERCADO_PAGO, null, null, LocalDate.now(), LocalDate.now(), desc, desc, amount, amount, "ARS", mt, catId, catName, Confidence.HIGH, status, "", "{}");
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
