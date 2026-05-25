package com.hogaria.service;

import com.hogaria.entity.ExternalLoanSyncConfig;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.repository.MoneyTransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.UUID;
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

  public ExternalLoanSyncEventProcessor(
      MoneyTransactionRepository transactionRepository,
      ExternalSyncIdempotencyService idempotencyService) {
    this.transactionRepository = transactionRepository;
    this.idempotencyService = idempotencyService;
  }

  public boolean isAlreadyProcessed(UUID userId, UUID profileId, String entityType, String entityId, String eventType) {
    return idempotencyService.isAlreadyProcessed(userId, profileId, SYSTEM, entityType, entityId, eventType);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean processDisbursement(UUID userId, UUID profileId, ExternalLoanSyncConfig cfg, String loanId, String personName, LocalDate date, BigDecimal amount) {
    String sourceOperationId = sourceOperationId(LOAN_ENTITY, loanId, DISBURSEMENT_EVENT);
    String sourceHash = sourceHash(profileId, cfg.getAccountId(), cfg.getLoanDisbursementCategoryId(), LOAN_ENTITY, loanId, DISBURSEMENT_EVENT, date, amount);
    if (idempotencyService.isAlreadyProcessed(userId, profileId, SYSTEM, LOAN_ENTITY, loanId, DISBURSEMENT_EVENT)
        || !transactionRepository.findByProfileIdAndSourceAndSourceOperationId(profileId, SYSTEM, sourceOperationId).isEmpty()
        || transactionRepository.existsByProfileIdAndSourceHash(profileId, sourceHash)) {
      return false;
    }
    MoneyTransaction loanTx =
        transactionRepository.save(
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
                .sourceOperationId(sourceOperationId(LOAN_ENTITY, loanId, DISBURSEMENT_EVENT))
                .sourceHash(sourceHash(profileId, cfg.getAccountId(), cfg.getLoanDisbursementCategoryId(), LOAN_ENTITY, loanId, DISBURSEMENT_EVENT, date, amount))
                .classificationReason("CJPRESTAMOS_DISBURSEMENT")
                .status(MoneyTransaction.Status.CONFIRMED)
                .build());
    markProcessed(profileId, LOAN_ENTITY, loanId, DISBURSEMENT_EVENT, sourceHash, loanTx.getId());
    return true;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean processPaymentPrincipal(UUID userId, UUID profileId, ExternalLoanSyncConfig cfg, String paymentId, LocalDate date, BigDecimal principalRecovered) {
    if (principalRecovered == null || principalRecovered.signum() <= 0) return false;
    String sourceOperationId = sourceOperationId(PAYMENT_ENTITY, paymentId, PAYMENT_PRINCIPAL_RECOVERY_EVENT);
    String sourceHash = sourceHash(profileId, cfg.getAccountId(), cfg.getPrincipalRecoveryCategoryId(), PAYMENT_ENTITY, paymentId, PAYMENT_PRINCIPAL_RECOVERY_EVENT, date, principalRecovered);
    if (idempotencyService.isAlreadyProcessed(userId, profileId, SYSTEM, PAYMENT_ENTITY, paymentId, PAYMENT_PRINCIPAL_RECOVERY_EVENT)
        || !transactionRepository.findByProfileIdAndSourceAndSourceOperationId(profileId, SYSTEM, sourceOperationId).isEmpty()
        || transactionRepository.existsByProfileIdAndSourceHash(profileId, sourceHash)) {
      return false;
    }
    MoneyTransaction principalTx =
        transactionRepository.save(
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
                .sourceOperationId(sourceOperationId(PAYMENT_ENTITY, paymentId, PAYMENT_PRINCIPAL_RECOVERY_EVENT))
                .sourceHash(sourceHash(profileId, cfg.getAccountId(), cfg.getPrincipalRecoveryCategoryId(), PAYMENT_ENTITY, paymentId, PAYMENT_PRINCIPAL_RECOVERY_EVENT, date, principalRecovered))
                .classificationReason("CJPRESTAMOS_PAYMENT_PRINCIPAL_RECOVERY")
                .status(MoneyTransaction.Status.CONFIRMED)
                .build());
    markProcessed(profileId, PAYMENT_ENTITY, paymentId, PAYMENT_PRINCIPAL_RECOVERY_EVENT, sourceHash, principalTx.getId());
    return true;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean processPaymentInterest(UUID userId, UUID profileId, ExternalLoanSyncConfig cfg, String paymentId, LocalDate date, BigDecimal interestCollected) {
    if (interestCollected == null || interestCollected.signum() <= 0) return false;
    String sourceOperationId = sourceOperationId(PAYMENT_ENTITY, paymentId, PAYMENT_INTEREST_INCOME_EVENT);
    String sourceHash = sourceHash(profileId, cfg.getAccountId(), cfg.getInterestIncomeCategoryId(), PAYMENT_ENTITY, paymentId, PAYMENT_INTEREST_INCOME_EVENT, date, interestCollected);
    if (idempotencyService.isAlreadyProcessed(userId, profileId, SYSTEM, PAYMENT_ENTITY, paymentId, PAYMENT_INTEREST_INCOME_EVENT)
        || !transactionRepository.findByProfileIdAndSourceAndSourceOperationId(profileId, SYSTEM, sourceOperationId).isEmpty()
        || transactionRepository.existsByProfileIdAndSourceHash(profileId, sourceHash)) {
      return false;
    }
    MoneyTransaction interestTx =
        transactionRepository.save(
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
                .sourceOperationId(sourceOperationId(PAYMENT_ENTITY, paymentId, PAYMENT_INTEREST_INCOME_EVENT))
                .sourceHash(sourceHash(profileId, cfg.getAccountId(), cfg.getInterestIncomeCategoryId(), PAYMENT_ENTITY, paymentId, PAYMENT_INTEREST_INCOME_EVENT, date, interestCollected))
                .classificationReason("CJPRESTAMOS_PAYMENT_INTEREST_INCOME")
                .status(MoneyTransaction.Status.CONFIRMED)
                .build());
    markProcessed(profileId, PAYMENT_ENTITY, paymentId, PAYMENT_INTEREST_INCOME_EVENT, sourceHash, interestTx.getId());
    return true;
  }

  private void markProcessed(UUID profileId, String entityType, String entityId, String eventType, String eventHash, UUID transactionId) {
    idempotencyService.markProcessed(profileId, SYSTEM, entityType, entityId, eventType, eventHash, transactionId, null);
  }

  private String sourceOperationId(String entityType, String entityId, String eventType) {
    if (LOAN_ENTITY.equals(entityType)) return "LOAN:" + entityId + ":" + eventType;
    if (PAYMENT_ENTITY.equals(entityType)) return "PAYMENT:" + entityId + ":" + (PAYMENT_PRINCIPAL_RECOVERY_EVENT.equals(eventType) ? "PRINCIPAL" : "INTEREST");
    return entityType + ":" + entityId + ":" + eventType;
  }

  private String sourceHash(UUID profileId, UUID accountId, UUID categoryId, String entityType, String entityId, String eventType, LocalDate date, BigDecimal amount) {
    String normalizedAmount = amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    String raw = String.join("|", profileId.toString(), SYSTEM, entityType, entityId, eventType, normalizedAmount, date.toString(), String.valueOf(accountId), String.valueOf(categoryId));
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder();
      for (byte b : hash) sb.append(String.format("%02x", b));
      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }


}
