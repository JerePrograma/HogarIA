package com.hogaria.service;

import com.hogaria.entity.Category;
import com.hogaria.entity.ExternalLoanSyncConfig;
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

  private final CjPrestamosClient client;
  private final CjPrestamosProperties properties;
  private final FinancialProfileRepository profileRepository;
  private final CjPrestamosBridgeMapper mapper;
  private final ExternalLoanSyncConfigRepository syncConfigRepository;
  private final AccountRepository accountRepository;
  private final CategoryRepository categoryRepository;
  private final ExternalLoanSyncEventProcessor eventProcessor;

  public ExternalLoansService(CjPrestamosClient client, CjPrestamosProperties properties, FinancialProfileRepository profileRepository, CjPrestamosBridgeMapper mapper, ExternalLoanSyncConfigRepository syncConfigRepository, AccountRepository accountRepository, CategoryRepository categoryRepository, ExternalLoanSyncEventProcessor eventProcessor) {
    this.client = client;
    this.properties = properties;
    this.profileRepository = profileRepository;
    this.mapper = mapper;
    this.syncConfigRepository = syncConfigRepository;
    this.accountRepository = accountRepository;
    this.categoryRepository = categoryRepository;
    this.eventProcessor = eventProcessor;
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
    return runSync(userId, profileId, false);
  }

  public ExternalLoanManualSyncResponse dryRunSync(UUID userId, UUID profileId) {
    return runSync(userId, profileId, true);
  }

  private ExternalLoanManualSyncResponse runSync(UUID userId, UUID profileId, boolean dryRun) {
    ensureProfileBelongsToUser(userId, profileId);
    if (!dryRun && !properties.syncEnabled()) throw new BadRequestException("La sincronización contable está deshabilitada. La integración está en modo solo lectura.");
    ExternalLoanSyncConfig cfg = syncConfigRepository.findByProfileId(profileId)
        .orElseThrow(() -> new BadRequestException("No existe configuración de sincronización externa"));
    if (!Boolean.TRUE.equals(cfg.getEnabled())) throw new BadRequestException("La sincronización externa está deshabilitada");
    validateCompleteConfig(cfg);
    validateConfigReferences(profileId, cfg);

    client.getDashboardSummary(profileId, userId);
    client.getCashControl(profileId, userId);
    List<CjPrestamosLoanActiveRemoteResponse> loans = client.getActiveLoans(profileId, userId);

    int loansSynced = 0, paymentsSynced = 0, movementsCreated = 0, skippedDuplicates = 0;
    int disbursement = 0, paymentPrincipalRecovery = 0, paymentInterestIncome = 0;
    List<String> errors = new ArrayList<>();
    List<String> detectedLoans = new ArrayList<>();
    List<String> detectedPayments = new ArrayList<>();
    List<String> plannedMovements = new ArrayList<>();

    for (CjPrestamosLoanActiveRemoteResponse loan : loans) {
      String loanId = String.valueOf(loan.id());
      detectedLoans.add(loanId);
      try {
        boolean created = dryRun
            ? !isDuplicate(userId, profileId, "LOAN", loanId, "DISBURSEMENT")
            : eventProcessor.processDisbursement(userId, profileId, cfg, loanId, loan.personaNombre(), loan.createdAt().toLocalDate(), loan.montoInicial());
        if (created) {
          movementsCreated++;
          loansSynced++;
          disbursement++;
          plannedMovements.add("DISBURSEMENT loan " + loanId + " amount=" + loan.montoInicial());
        } else {
          skippedDuplicates++;
        }
      } catch (Exception ex) { errors.add("loan " + loanId + ": " + ex.getMessage()); }

      List<CjPrestamosPaymentRemoteResponse> payments = client.getLoanPayments(profileId, userId, loan.id());
      for (CjPrestamosPaymentRemoteResponse payment : payments) {
        String paymentId = String.valueOf(payment.id());
        detectedPayments.add(paymentId);
        try {
          BigDecimal principalRecovered = payment.principalRecovered();
          BigDecimal interestCollected = payment.interestCollected();
          if (principalRecovered == null || interestCollected == null) {
            throw new BadRequestException("cjprestamos no envió split principal/interés para pago " + paymentId);
          }

          boolean createdPrincipal = dryRun
              ? (principalRecovered.signum() > 0 && !isDuplicate(userId, profileId, "PAYMENT", paymentId, "PAYMENT_PRINCIPAL_RECOVERY"))
              : eventProcessor.processPaymentPrincipal(userId, profileId, cfg, paymentId, payment.fechaPago(), principalRecovered);
          boolean createdInterest = dryRun
              ? (interestCollected.signum() > 0 && !isDuplicate(userId, profileId, "PAYMENT", paymentId, "PAYMENT_INTEREST_INCOME"))
              : eventProcessor.processPaymentInterest(userId, profileId, cfg, paymentId, payment.fechaPago(), interestCollected);

          if (principalRecovered.signum() > 0 && !createdPrincipal) skippedDuplicates++;
          if (interestCollected.signum() > 0 && !createdInterest) skippedDuplicates++;
          if (createdPrincipal || createdInterest) {
            paymentsSynced++;
            movementsCreated += createdPrincipal ? 1 : 0;
            movementsCreated += createdInterest ? 1 : 0;
            if (createdPrincipal) {
              paymentPrincipalRecovery++;
              plannedMovements.add("PAYMENT_PRINCIPAL_RECOVERY payment " + paymentId + " amount=" + principalRecovered);
            }
            if (createdInterest) {
              paymentInterestIncome++;
              plannedMovements.add("PAYMENT_INTEREST_INCOME payment " + paymentId + " amount=" + interestCollected);
            }
          }
        } catch (Exception ex) { errors.add("payment " + paymentId + ": " + ex.getMessage()); }
      }
    }
    return new ExternalLoanManualSyncResponse(dryRun, loansSynced, paymentsSynced, movementsCreated, skippedDuplicates, errors, detectedLoans, detectedPayments, plannedMovements,
        java.util.Map.of("DISBURSEMENT", disbursement, "PAYMENT_PRINCIPAL_RECOVERY", paymentPrincipalRecovery, "PAYMENT_INTEREST_INCOME", paymentInterestIncome));
  }

  private boolean isDuplicate(UUID userId, UUID profileId, String entityType, String entityId, String eventType) {
    return eventProcessor.isAlreadyProcessed(userId, profileId, entityType, entityId, eventType);
  }


  private void validateCompleteConfig(ExternalLoanSyncConfig cfg) {
    if (cfg.getAccountId() == null
        || cfg.getLoanDisbursementCategoryId() == null
        || cfg.getPrincipalRecoveryCategoryId() == null
        || cfg.getInterestIncomeCategoryId() == null) {
      throw new BadRequestException("Configuración de sincronización incompleta");
    }
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
