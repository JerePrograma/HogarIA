package com.hogaria.service;

import com.hogaria.entity.ExternalLoanSyncConfig;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.repository.MoneyTransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExternalLoanSyncEventProcessor {
  private static final String SYSTEM = "CJPRESTAMOS";
  private static final String LOAN_ENTITY = "LOAN";
  private static final String PAYMENT_ENTITY = "PAYMENT";
  private static final String PAYMENT_PRINCIPAL_RECOVERY_EVENT = "PAYMENT_PRINCIPAL_RECOVERY";
  private static final String PAYMENT_INTEREST_INCOME_EVENT = "PAYMENT_INTEREST_INCOME";
  private static final String DISBURSEMENT_EVENT = "DISBURSEMENT";

  private final MoneyTransactionRepository transactionRepository;
  private final ExternalSyncIdempotencyService idempotencyService;
  private final ExternalLoanEventDuplicateDetector duplicateDetector;

  public ExternalLoanSyncEventProcessor(
      MoneyTransactionRepository transactionRepository,
      ExternalSyncIdempotencyService idempotencyService,
      ExternalLoanEventDuplicateDetector duplicateDetector) {
    this.transactionRepository = transactionRepository;
    this.idempotencyService = idempotencyService;
    this.duplicateDetector = duplicateDetector;
  }

  public boolean isAlreadyProcessed(UUID userId, UUID profileId, String entityType, String entityId, String eventType) {
    return idempotencyService.isAlreadyProcessed(userId, profileId, SYSTEM, entityType, entityId, eventType);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean processDisbursement(UUID userId, UUID profileId, ExternalLoanSyncConfig cfg, String loanId, String personName, LocalDate date, BigDecimal amount) {
    var duplicate =
        duplicateDetector.detect(
            userId,
            profileId,
            cfg.getAccountId(),
            cfg.getLoanDisbursementCategoryId(),
            LOAN_ENTITY,
            loanId,
            DISBURSEMENT_EVENT,
            date,
            amount);
    if (duplicate.duplicate()) {
      return false;
    }
    MoneyTransaction loanTx =
        saveOrSkipDuplicate(
            userId,
            profileId,
            cfg.getAccountId(),
            cfg.getLoanDisbursementCategoryId(),
            LOAN_ENTITY,
            loanId,
            DISBURSEMENT_EVENT,
            date,
            amount,
            MoneyTransaction.builder()
                .profileId(profileId)
                .accountId(cfg.getAccountId())
                .categoryId(cfg.getLoanDisbursementCategoryId())
                .movementType(MoneyTransaction.MovementType.ADJUSTMENT)
                .realDate(date)
                .budgetDate(date)
                .amount(amount)
                .currency("ARS")
                .origin(MoneyTransaction.Origin.SYSTEM)
                .description("Préstamo CJ #" + loanId + " - " + personName)
                .source(SYSTEM)
                .sourceOperationId(duplicate.sourceOperationId())
                .sourceHash(duplicate.sourceHash())
                .classificationReason("CJPRESTAMOS_DISBURSEMENT")
                .status(MoneyTransaction.Status.CONFIRMED)
                .build());
    if (loanTx == null) {
      return false;
    }
    markProcessed(profileId, LOAN_ENTITY, loanId, DISBURSEMENT_EVENT, duplicate.sourceHash(), loanTx.getId());
    return true;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean processPaymentPrincipal(UUID userId, UUID profileId, ExternalLoanSyncConfig cfg, String paymentId, LocalDate date, BigDecimal principalRecovered) {
    if (principalRecovered == null || principalRecovered.signum() <= 0) return false;
    var duplicate =
        duplicateDetector.detect(
            userId,
            profileId,
            cfg.getAccountId(),
            cfg.getPrincipalRecoveryCategoryId(),
            PAYMENT_ENTITY,
            paymentId,
            PAYMENT_PRINCIPAL_RECOVERY_EVENT,
            date,
            principalRecovered);
    if (duplicate.duplicate()) {
      return false;
    }
    MoneyTransaction principalTx =
        saveOrSkipDuplicate(
            userId,
            profileId,
            cfg.getAccountId(),
            cfg.getPrincipalRecoveryCategoryId(),
            PAYMENT_ENTITY,
            paymentId,
            PAYMENT_PRINCIPAL_RECOVERY_EVENT,
            date,
            principalRecovered,
            MoneyTransaction.builder()
                .profileId(profileId)
                .accountId(cfg.getAccountId())
                .categoryId(cfg.getPrincipalRecoveryCategoryId())
                .movementType(MoneyTransaction.MovementType.ADJUSTMENT)
                .realDate(date)
                .budgetDate(date)
                .amount(principalRecovered)
                .currency("ARS")
                .origin(MoneyTransaction.Origin.SYSTEM)
                .description("Recupero capital CJ pago #" + paymentId)
                .source(SYSTEM)
                .sourceOperationId(duplicate.sourceOperationId())
                .sourceHash(duplicate.sourceHash())
                .classificationReason("CJPRESTAMOS_PAYMENT_PRINCIPAL_RECOVERY")
                .status(MoneyTransaction.Status.CONFIRMED)
                .build());
    if (principalTx == null) {
      return false;
    }
    markProcessed(
        profileId,
        PAYMENT_ENTITY,
        paymentId,
        PAYMENT_PRINCIPAL_RECOVERY_EVENT,
        duplicate.sourceHash(),
        principalTx.getId());
    return true;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean processPaymentInterest(UUID userId, UUID profileId, ExternalLoanSyncConfig cfg, String paymentId, LocalDate date, BigDecimal interestCollected) {
    if (interestCollected == null || interestCollected.signum() <= 0) return false;
    var duplicate =
        duplicateDetector.detect(
            userId,
            profileId,
            cfg.getAccountId(),
            cfg.getInterestIncomeCategoryId(),
            PAYMENT_ENTITY,
            paymentId,
            PAYMENT_INTEREST_INCOME_EVENT,
            date,
            interestCollected);
    if (duplicate.duplicate()) {
      return false;
    }
    MoneyTransaction interestTx =
        saveOrSkipDuplicate(
            userId,
            profileId,
            cfg.getAccountId(),
            cfg.getInterestIncomeCategoryId(),
            PAYMENT_ENTITY,
            paymentId,
            PAYMENT_INTEREST_INCOME_EVENT,
            date,
            interestCollected,
            MoneyTransaction.builder()
                .profileId(profileId)
                .accountId(cfg.getAccountId())
                .categoryId(cfg.getInterestIncomeCategoryId())
                .movementType(MoneyTransaction.MovementType.INCOME)
                .realDate(date)
                .budgetDate(date)
                .amount(interestCollected)
                .currency("ARS")
                .origin(MoneyTransaction.Origin.SYSTEM)
                .description("Interés CJ pago #" + paymentId)
                .source(SYSTEM)
                .sourceOperationId(duplicate.sourceOperationId())
                .sourceHash(duplicate.sourceHash())
                .classificationReason("CJPRESTAMOS_PAYMENT_INTEREST_INCOME")
                .status(MoneyTransaction.Status.CONFIRMED)
                .build());
    if (interestTx == null) {
      return false;
    }
    markProcessed(
        profileId,
        PAYMENT_ENTITY,
        paymentId,
        PAYMENT_INTEREST_INCOME_EVENT,
        duplicate.sourceHash(),
        interestTx.getId());
    return true;
  }

  private MoneyTransaction saveOrSkipDuplicate(
      UUID userId,
      UUID profileId,
      UUID accountId,
      UUID categoryId,
      String entityType,
      String entityId,
      String eventType,
      LocalDate date,
      BigDecimal amount,
      MoneyTransaction transaction) {
    try {
      return transactionRepository.saveAndFlush(transaction);
    } catch (DataIntegrityViolationException ex) {
      var duplicate =
          duplicateDetector.detect(
              userId, profileId, accountId, categoryId, entityType, entityId, eventType, date, amount);
      if (duplicate.duplicate()) {
        return null;
      }
      throw ex;
    }
  }

  private void markProcessed(UUID profileId, String entityType, String entityId, String eventType, String eventHash, UUID transactionId) {
    idempotencyService.markProcessed(profileId, SYSTEM, entityType, entityId, eventType, eventHash, transactionId, null);
  }

}
