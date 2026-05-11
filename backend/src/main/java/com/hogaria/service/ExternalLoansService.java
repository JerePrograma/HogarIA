package com.hogaria.service;

import com.hogaria.entity.Category;
import com.hogaria.entity.ExternalLoanSyncConfig;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.exception.BadRequestException;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.integration.cjprestamos.CjPrestamosClient;
import com.hogaria.integration.cjprestamos.CjPrestamosProperties;
import com.hogaria.integration.cjprestamos.dto.ExternalLoanManualSyncResponse;
import com.hogaria.integration.cjprestamos.dto.ExternalLoansSummaryResponse;
import com.hogaria.integration.cjprestamos.mapper.CjPrestamosBridgeMapper;
import com.hogaria.integration.cjprestamos.remote.CjPrestamosLoanActiveRemoteResponse;
import com.hogaria.integration.cjprestamos.remote.CjPrestamosPaymentRemoteResponse;
import com.hogaria.repository.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ExternalLoansService {
  private static final String SYSTEM = "CJPRESTAMOS";
  private static final String LOAN_ENTITY = "LOAN";
  private static final String PAYMENT_ENTITY = "PAYMENT";
  private static final String PAYMENT_PRINCIPAL_RECOVERY_EVENT = "PAYMENT_PRINCIPAL_RECOVERY";
  private static final String PAYMENT_INTEREST_INCOME_EVENT = "PAYMENT_INTEREST_INCOME";

  private final CjPrestamosClient client;
  private final CjPrestamosProperties properties;
  private final FinancialProfileRepository profileRepository;
  private final CjPrestamosBridgeMapper mapper;
  private final ExternalLoanSyncConfigRepository syncConfigRepository;
  private final AccountRepository accountRepository;
  private final CategoryRepository categoryRepository;
  private final MoneyTransactionRepository transactionRepository;
  private final ExternalSyncIdempotencyService idempotencyService;

  public ExternalLoansService(CjPrestamosClient client, CjPrestamosProperties properties, FinancialProfileRepository profileRepository, CjPrestamosBridgeMapper mapper, ExternalLoanSyncConfigRepository syncConfigRepository, AccountRepository accountRepository, CategoryRepository categoryRepository, MoneyTransactionRepository transactionRepository, ExternalSyncIdempotencyService idempotencyService) {
    this.client = client;
    this.properties = properties;
    this.profileRepository = profileRepository;
    this.mapper = mapper;
    this.syncConfigRepository = syncConfigRepository;
    this.accountRepository = accountRepository;
    this.categoryRepository = categoryRepository;
    this.transactionRepository = transactionRepository;
    this.idempotencyService = idempotencyService;
  }

  public ExternalLoansSummaryResponse getSummary(UUID userId, UUID profileId) {
    ensureProfileBelongsToUser(userId, profileId);
    if (!properties.enabled()) return ExternalLoansSummaryResponse.disabled();
    return ExternalLoansSummaryResponse.enabled(
        mapper.toExternalDashboard(client.getDashboardSummary(profileId, userId)),
        mapper.toExternalCashControl(client.getCashControl(profileId, userId)),
        mapper.toExternalLoans(client.getActiveLoans(profileId, userId)),
        !properties.syncEnabled());
  }

  public ExternalLoanManualSyncResponse sync(UUID userId, UUID profileId) {
    ensureProfileBelongsToUser(userId, profileId);
    if (!properties.syncEnabled()) throw new BadRequestException("La sincronización contable está deshabilitada. La integración está en modo solo lectura.");
    ExternalLoanSyncConfig cfg = syncConfigRepository.findByProfileId(profileId)
        .orElseThrow(() -> new BadRequestException("No existe configuración de sincronización externa"));
    if (!Boolean.TRUE.equals(cfg.getEnabled())) throw new BadRequestException("La sincronización externa está deshabilitada");
    validateConfigReferences(profileId, cfg);

    client.getDashboardSummary(profileId, userId);
    client.getCashControl(profileId, userId);
    List<CjPrestamosLoanActiveRemoteResponse> loans = client.getActiveLoans(profileId, userId);

    int loansSynced = 0, paymentsSynced = 0, movementsCreated = 0, skippedDuplicates = 0;
    List<String> errors = new ArrayList<>();

    for (CjPrestamosLoanActiveRemoteResponse loan : loans) {
      String loanId = String.valueOf(loan.id());
      try {
        if (idempotencyService.isAlreadyProcessed(userId, profileId, SYSTEM, LOAN_ENTITY, loanId, "DISBURSEMENT")) {
          skippedDuplicates++;
        } else {
        MoneyTransaction loanTx = transactionRepository.save(MoneyTransaction.builder()
            .profileId(profileId).accountId(cfg.getAccountId()).categoryId(cfg.getLoanDisbursementCategoryId())
            .movementType(MoneyTransaction.MovementType.EXPENSE)
            .realDate(loan.createdAt().toLocalDate()).budgetDate(loan.createdAt().toLocalDate())
            .amount(loan.montoInicial()).currency("ARS").origin(MoneyTransaction.Origin.SYSTEM)
            .description("Préstamo CJ #" + loan.id() + " - " + loan.personaNombre())
            .status(MoneyTransaction.Status.CONFIRMED).build());
        movementsCreated++; loansSynced++;
        idempotencyService.markProcessed(profileId, SYSTEM, LOAN_ENTITY, loanId, "DISBURSEMENT", "loan-" + loanId, loanTx.getId(), null);
        }
      } catch (Exception ex) { errors.add("loan " + loanId + ": " + ex.getMessage()); }

      List<CjPrestamosPaymentRemoteResponse> payments = client.getLoanPayments(profileId, userId, loan.id());
      for (CjPrestamosPaymentRemoteResponse payment : payments) {
        String paymentId = String.valueOf(payment.id());
        try {
          BigDecimal principalRecovered = payment.principalRecovered();
          BigDecimal interestCollected = payment.interestCollected();
          if (principalRecovered == null || interestCollected == null) {
            throw new BadRequestException("cjprestamos no envió split principal/interés para pago " + paymentId);
          }

          boolean paymentDuplicate = true;

          if (principalRecovered.signum() > 0) {
            if (idempotencyService.isAlreadyProcessed(userId, profileId, SYSTEM, PAYMENT_ENTITY, paymentId, PAYMENT_PRINCIPAL_RECOVERY_EVENT)) {
              skippedDuplicates++;
            } else {
              MoneyTransaction principalTx = transactionRepository.save(MoneyTransaction.builder().profileId(profileId).accountId(cfg.getAccountId())
                  .categoryId(cfg.getPrincipalRecoveryCategoryId()).movementType(MoneyTransaction.MovementType.ADJUSTMENT)
                  .realDate(payment.fechaPago()).budgetDate(payment.fechaPago()).amount(principalRecovered)
                  .currency("ARS").origin(MoneyTransaction.Origin.SYSTEM)
                  .description("Recupero capital CJ pago #" + payment.id()).status(MoneyTransaction.Status.CONFIRMED).build());
              movementsCreated++;
              paymentDuplicate = false;
              idempotencyService.markProcessed(profileId, SYSTEM, PAYMENT_ENTITY, paymentId, PAYMENT_PRINCIPAL_RECOVERY_EVENT, "payment-principal-" + paymentId, principalTx.getId(), null);
            }
          }
          if (interestCollected.signum() > 0) {
            if (idempotencyService.isAlreadyProcessed(userId, profileId, SYSTEM, PAYMENT_ENTITY, paymentId, PAYMENT_INTEREST_INCOME_EVENT)) {
              skippedDuplicates++;
            } else {
              MoneyTransaction interestTx = transactionRepository.save(MoneyTransaction.builder().profileId(profileId).accountId(cfg.getAccountId())
                  .categoryId(cfg.getInterestIncomeCategoryId()).movementType(MoneyTransaction.MovementType.INCOME)
                  .realDate(payment.fechaPago()).budgetDate(payment.fechaPago()).amount(interestCollected)
                  .currency("ARS").origin(MoneyTransaction.Origin.SYSTEM)
                  .description("Interés CJ pago #" + payment.id()).status(MoneyTransaction.Status.CONFIRMED).build());
              movementsCreated++;
              paymentDuplicate = false;
              idempotencyService.markProcessed(profileId, SYSTEM, PAYMENT_ENTITY, paymentId, PAYMENT_INTEREST_INCOME_EVENT, "payment-interest-" + paymentId, interestTx.getId(), null);
            }
          }
          if (!paymentDuplicate) paymentsSynced++;
        } catch (Exception ex) { errors.add("payment " + paymentId + ": " + ex.getMessage()); }
      }
    }
    return new ExternalLoanManualSyncResponse(loansSynced, paymentsSynced, movementsCreated, skippedDuplicates, errors);
  }

  private void validateConfigReferences(UUID profileId, ExternalLoanSyncConfig cfg) {
    if (!accountRepository.existsByIdAndProfileId(cfg.getAccountId(), profileId)) throw new BadRequestException("La cuenta configurada no pertenece al perfil");
    validateCategory(profileId, cfg.getLoanDisbursementCategoryId());
    validateCategory(profileId, cfg.getPrincipalRecoveryCategoryId());
    validateCategory(profileId, cfg.getInterestIncomeCategoryId());
  }

  private void validateCategory(UUID profileId, UUID categoryId) {
    Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new BadRequestException("Categoría configurada inexistente"));
    if (category.getProfileId() != null && !category.getProfileId().equals(profileId)) throw new BadRequestException("Categoría configurada no pertenece al perfil");
  }

  private void ensureProfileBelongsToUser(UUID userId, UUID profileId) {
    if (!profileRepository.existsByIdAndUserId(profileId, userId)) throw new ForbiddenException("Profile no pertenece al usuario");
  }
}
