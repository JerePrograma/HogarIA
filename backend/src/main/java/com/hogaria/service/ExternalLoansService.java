package com.hogaria.service;

import com.hogaria.entity.Category;
import com.hogaria.entity.ExternalLoanSyncConfig;
import com.hogaria.exception.BadRequestException;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.integration.cjprestamos.CjPrestamosClient;
import com.hogaria.integration.cjprestamos.CjPrestamosIntegrationConfigValidator;
import com.hogaria.integration.cjprestamos.CjPrestamosIntegrationException;
import com.hogaria.integration.cjprestamos.CjPrestamosProperties;
import com.hogaria.integration.cjprestamos.dto.ExternalIntegrationDiagnosticResponse;
import com.hogaria.integration.cjprestamos.dto.ExternalLoanManualSyncResponse;
import com.hogaria.integration.cjprestamos.dto.ExternalLoansSummaryResponse;
import com.hogaria.integration.cjprestamos.mapper.CjPrestamosBridgeMapper;
import com.hogaria.integration.cjprestamos.remote.CjPrestamosLoanActiveRemoteResponse;
import com.hogaria.integration.cjprestamos.remote.CjPrestamosPaymentRemoteResponse;
import com.hogaria.repository.AccountRepository;
import com.hogaria.repository.CategoryRepository;
import com.hogaria.repository.ExternalLoanSyncConfigRepository;
import com.hogaria.repository.FinancialProfileRepository;
import com.hogaria.service.ExternalLoanEventDuplicateDetector.DuplicateDetectionResult;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ExternalLoansService {
  private static final String LOAN_ENTITY = "LOAN";
  private static final String PAYMENT_ENTITY = "PAYMENT";
  private static final String DISBURSEMENT_EVENT = "DISBURSEMENT";
  private static final String PAYMENT_PRINCIPAL_RECOVERY_EVENT = "PAYMENT_PRINCIPAL_RECOVERY";
  private static final String PAYMENT_INTEREST_INCOME_EVENT = "PAYMENT_INTEREST_INCOME";

  private static final Logger log = LoggerFactory.getLogger(ExternalLoansService.class);

  private final CjPrestamosClient client;
  private final CjPrestamosProperties properties;
  private final CjPrestamosIntegrationConfigValidator integrationConfigValidator;
  private final FinancialProfileRepository profileRepository;
  private final CjPrestamosBridgeMapper mapper;
  private final ExternalLoanSyncConfigRepository syncConfigRepository;
  private final AccountRepository accountRepository;
  private final CategoryRepository categoryRepository;
  private final ExternalLoanSyncEventProcessor eventProcessor;
  private final ExternalLoanEventDuplicateDetector duplicateDetector;

  public ExternalLoansService(
      CjPrestamosClient client,
      CjPrestamosProperties properties,
      FinancialProfileRepository profileRepository,
      CjPrestamosBridgeMapper mapper,
      ExternalLoanSyncConfigRepository syncConfigRepository,
      AccountRepository accountRepository,
      CategoryRepository categoryRepository,
      ExternalLoanSyncEventProcessor eventProcessor,
      CjPrestamosIntegrationConfigValidator integrationConfigValidator,
      ExternalLoanEventDuplicateDetector duplicateDetector) {
    this.client = client;
    this.properties = properties;
    this.profileRepository = profileRepository;
    this.mapper = mapper;
    this.syncConfigRepository = syncConfigRepository;
    this.accountRepository = accountRepository;
    this.categoryRepository = categoryRepository;
    this.eventProcessor = eventProcessor;
    this.integrationConfigValidator = integrationConfigValidator;
    this.duplicateDetector = duplicateDetector;
  }

  public ExternalLoansSummaryResponse getSummary(UUID userId, UUID profileId) {
    ensureProfileBelongsToUser(userId, profileId);
    if (!properties.enabled()) {
      return ExternalLoansSummaryResponse.disabled();
    }
    integrationConfigValidator.validateEnabledIntegration(properties);

    long startedAt = System.currentTimeMillis();
    log.info(
        "external-loans operation=summary profileId={} userId={} remoteBaseUrl={} apiPrefix={} status=START",
        profileId,
        userId,
        sanitizeUrl(properties.baseUrl()),
        properties.resolvedApiPrefix());

    var response =
        ExternalLoansSummaryResponse.enabled(
            mapper.toExternalDashboard(client.getDashboardSummary(profileId, userId)),
            mapper.toExternalCashControl(client.getCashControl(profileId, userId)),
            mapper.toExternalLoans(client.getActiveLoans(profileId, userId)),
            !properties.syncEnabled());

    log.info(
        "external-loans operation=summary profileId={} userId={} remoteBaseUrl={} apiPrefix={} durationMs={} status=OK createdMovements=0 skippedDuplicates=0 errors=0 loansCount={}",
        profileId,
        userId,
        sanitizeUrl(properties.baseUrl()),
        properties.resolvedApiPrefix(),
        System.currentTimeMillis() - startedAt,
        response.activeLoans().size());
    return response;
  }

  public ExternalIntegrationDiagnosticResponse checkIntegration(UUID userId, UUID profileId) {
    ensureProfileBelongsToUser(userId, profileId);
    long startedAt = System.currentTimeMillis();
    boolean integrationEnabled = properties.enabled();
    boolean syncEnabled = properties.syncEnabled();
    String apiPrefix = properties.resolvedApiPrefix();
    String baseUrl = sanitizeUrl(properties.baseUrl());
    boolean hasUsername = StringUtils.hasText(properties.username());
    boolean hasPassword = StringUtils.hasText(properties.password());
    List<String> missingFields = parseMissingFields();

    if (!integrationEnabled) {
      return diagnosticAndLog(
          "MISCONFIGURED",
          "Integración deshabilitada (CJP_INTEGRATION_ENABLED=false)",
          userId,
          profileId,
          startedAt,
          false,
          0,
          0,
          integrationEnabled,
          syncEnabled,
          baseUrl,
          apiPrefix,
          hasUsername,
          hasPassword,
          missingFields);
    }

    if (!missingFields.isEmpty()) {
      return diagnosticAndLog(
          "MISCONFIGURED",
          "Configuración incompleta. Faltan: " + String.join(", ", missingFields),
          userId,
          profileId,
          startedAt,
          false,
          0,
          0,
          integrationEnabled,
          syncEnabled,
          baseUrl,
          apiPrefix,
          hasUsername,
          hasPassword,
          missingFields);
    }

    try {
      client.getDashboardSummary(profileId, userId);
      return diagnosticAndLog(
          "OK",
          "Conectividad y autenticación OK",
          userId,
          profileId,
          startedAt,
          true,
          0,
          0,
          integrationEnabled,
          syncEnabled,
          baseUrl,
          apiPrefix,
          hasUsername,
          hasPassword,
          missingFields);
    } catch (CjPrestamosIntegrationException ex) {
      String message = ex.getMessage() == null ? "Error remoto" : ex.getMessage();
      String status = message.contains("Autenticación/autorización") ? "UNAUTHORIZED" : "UNAVAILABLE";
      return diagnosticAndLog(
          status,
          message,
          userId,
          profileId,
          startedAt,
          true,
          0,
          1,
          integrationEnabled,
          syncEnabled,
          baseUrl,
          apiPrefix,
          hasUsername,
          hasPassword,
          missingFields);
    }
  }

  public ExternalLoanManualSyncResponse sync(UUID userId, UUID profileId) {
    return runSync(userId, profileId, false);
  }

  public ExternalLoanManualSyncResponse dryRunSync(UUID userId, UUID profileId) {
    return runSync(userId, profileId, true);
  }

  private ExternalLoanManualSyncResponse runSync(UUID userId, UUID profileId, boolean dryRun) {
    ensureProfileBelongsToUser(userId, profileId);
    integrationConfigValidator.validateEnabledIntegration(properties);
    if (!dryRun && !properties.syncEnabled()) {
      throw new BadRequestException(
          "La sincronización contable está deshabilitada. La integración está en modo solo lectura.");
    }

    long startedAt = System.currentTimeMillis();
    String operation = dryRun ? "dry-run" : "sync";
    log.info(
        "external-loans operation={} profileId={} userId={} remoteBaseUrl={} apiPrefix={} status=START",
        operation,
        profileId,
        userId,
        sanitizeUrl(properties.baseUrl()),
        properties.resolvedApiPrefix());

    ExternalLoanSyncConfig cfg =
        syncConfigRepository
            .findByProfileId(profileId)
            .orElseThrow(() -> new BadRequestException("No existe configuración de sincronización externa"));
    if (!Boolean.TRUE.equals(cfg.getEnabled())) {
      throw new BadRequestException("La sincronización externa está deshabilitada");
    }
    validateCompleteConfig(cfg);
    validateConfigReferences(profileId, cfg);

    client.getDashboardSummary(profileId, userId);
    client.getCashControl(profileId, userId);
    List<CjPrestamosLoanActiveRemoteResponse> loans = client.getActiveLoans(profileId, userId);

    int loansSynced = 0;
    int paymentsSynced = 0;
    int movementsCreated = 0;
    int skippedDuplicates = 0;
    int detectedExistingWithoutMapping = 0;
    int disbursement = 0;
    int paymentPrincipalRecovery = 0;
    int paymentInterestIncome = 0;

    List<String> errors = new ArrayList<>();
    List<String> detectedLoans = new ArrayList<>();
    List<String> detectedPayments = new ArrayList<>();
    List<String> plannedMovements = new ArrayList<>();

    for (CjPrestamosLoanActiveRemoteResponse loan : loans) {
      String loanId = String.valueOf(loan.id());
      detectedLoans.add(loanId);
      try {
        DuplicateDetectionResult duplicate = null;
        boolean created;
        if (dryRun) {
          duplicate = detectDisbursement(userId, profileId, cfg, loanId, loan);
          created = !duplicate.duplicate();
        } else {
          created =
              eventProcessor.processDisbursement(
                  userId,
                  profileId,
                  cfg,
                  loanId,
                  loan.personaNombre(),
                  loan.createdAt().toLocalDate(),
                  loan.montoInicial());
          if (!created) {
            duplicate = detectDisbursement(userId, profileId, cfg, loanId, loan);
          }
        }

        if (created) {
          movementsCreated++;
          loansSynced++;
          disbursement++;
          plannedMovements.add("DISBURSEMENT loan " + loanId + " amount=" + loan.montoInicial());
        } else {
          skippedDuplicates++;
          if (duplicate != null && duplicate.existingTransactionWithoutMapping()) {
            detectedExistingWithoutMapping++;
          }
        }
      } catch (Exception ex) {
        errors.add("loan " + loanId + ": " + ex.getMessage());
      }

      List<CjPrestamosPaymentRemoteResponse> payments = client.getLoanPayments(profileId, userId, loan.id());
      for (CjPrestamosPaymentRemoteResponse payment : payments) {
        String paymentId = String.valueOf(payment.id());
        detectedPayments.add(paymentId);
        try {
          BigDecimal principalRecovered = payment.principalRecovered();
          BigDecimal interestCollected = payment.interestCollected();
          if (principalRecovered == null || interestCollected == null) {
            throw new BadRequestException(
                "cjprestamos no envió split principal/interés para pago " + paymentId);
          }

          DuplicateDetectionResult principalDuplicate = null;
          DuplicateDetectionResult interestDuplicate = null;
          boolean hasPrincipal = principalRecovered.signum() > 0;
          boolean hasInterest = interestCollected.signum() > 0;
          boolean createdPrincipal;
          boolean createdInterest;

          if (dryRun) {
            principalDuplicate =
                hasPrincipal
                    ? detectPaymentPrincipal(userId, profileId, cfg, paymentId, payment, principalRecovered)
                    : null;
            interestDuplicate =
                hasInterest
                    ? detectPaymentInterest(userId, profileId, cfg, paymentId, payment, interestCollected)
                    : null;
            createdPrincipal = hasPrincipal && !principalDuplicate.duplicate();
            createdInterest = hasInterest && !interestDuplicate.duplicate();
          } else {
            createdPrincipal =
                eventProcessor.processPaymentPrincipal(
                    userId, profileId, cfg, paymentId, payment.fechaPago(), principalRecovered);
            createdInterest =
                eventProcessor.processPaymentInterest(
                    userId, profileId, cfg, paymentId, payment.fechaPago(), interestCollected);
            if (hasPrincipal && !createdPrincipal) {
              principalDuplicate =
                  detectPaymentPrincipal(userId, profileId, cfg, paymentId, payment, principalRecovered);
            }
            if (hasInterest && !createdInterest) {
              interestDuplicate =
                  detectPaymentInterest(userId, profileId, cfg, paymentId, payment, interestCollected);
            }
          }

          if (hasPrincipal && !createdPrincipal) {
            skippedDuplicates++;
            if (principalDuplicate != null && principalDuplicate.existingTransactionWithoutMapping()) {
              detectedExistingWithoutMapping++;
            }
          }
          if (hasInterest && !createdInterest) {
            skippedDuplicates++;
            if (interestDuplicate != null && interestDuplicate.existingTransactionWithoutMapping()) {
              detectedExistingWithoutMapping++;
            }
          }

          if (createdPrincipal || createdInterest) {
            paymentsSynced++;
            movementsCreated += createdPrincipal ? 1 : 0;
            movementsCreated += createdInterest ? 1 : 0;
            if (createdPrincipal) {
              paymentPrincipalRecovery++;
              plannedMovements.add(
                  "PAYMENT_PRINCIPAL_RECOVERY payment " + paymentId + " amount=" + principalRecovered);
            }
            if (createdInterest) {
              paymentInterestIncome++;
              plannedMovements.add(
                  "PAYMENT_INTEREST_INCOME payment " + paymentId + " amount=" + interestCollected);
            }
          }
        } catch (Exception ex) {
          errors.add("payment " + paymentId + ": " + ex.getMessage());
        }
      }
    }

    log.info(
        "external-loans operation={} profileId={} userId={} remoteBaseUrl={} apiPrefix={} durationMs={} status={} createdMovements={} skippedDuplicates={} errors={} loans={} payments={}",
        operation,
        profileId,
        userId,
        sanitizeUrl(properties.baseUrl()),
        properties.resolvedApiPrefix(),
        System.currentTimeMillis() - startedAt,
        errors.isEmpty() ? "OK" : "PARTIAL",
        movementsCreated,
        skippedDuplicates,
        errors.size(),
        detectedLoans.size(),
        detectedPayments.size());

    return new ExternalLoanManualSyncResponse(
        dryRun,
        loansSynced,
        paymentsSynced,
        movementsCreated,
        skippedDuplicates,
        detectedExistingWithoutMapping,
        detectedExistingWithoutMapping > 0,
        errors,
        detectedLoans,
        detectedPayments,
        plannedMovements,
        Map.of(
            DISBURSEMENT_EVENT,
            disbursement,
            PAYMENT_PRINCIPAL_RECOVERY_EVENT,
            paymentPrincipalRecovery,
            PAYMENT_INTEREST_INCOME_EVENT,
            paymentInterestIncome));
  }

  private DuplicateDetectionResult detectDisbursement(
      UUID userId,
      UUID profileId,
      ExternalLoanSyncConfig cfg,
      String loanId,
      CjPrestamosLoanActiveRemoteResponse loan) {
    return duplicateDetector.detect(
        userId,
        profileId,
        cfg.getAccountId(),
        cfg.getLoanDisbursementCategoryId(),
        LOAN_ENTITY,
        loanId,
        DISBURSEMENT_EVENT,
        loan.createdAt().toLocalDate(),
        loan.montoInicial());
  }

  private DuplicateDetectionResult detectPaymentPrincipal(
      UUID userId,
      UUID profileId,
      ExternalLoanSyncConfig cfg,
      String paymentId,
      CjPrestamosPaymentRemoteResponse payment,
      BigDecimal principalRecovered) {
    return duplicateDetector.detect(
        userId,
        profileId,
        cfg.getAccountId(),
        cfg.getPrincipalRecoveryCategoryId(),
        PAYMENT_ENTITY,
        paymentId,
        PAYMENT_PRINCIPAL_RECOVERY_EVENT,
        payment.fechaPago(),
        principalRecovered);
  }

  private DuplicateDetectionResult detectPaymentInterest(
      UUID userId,
      UUID profileId,
      ExternalLoanSyncConfig cfg,
      String paymentId,
      CjPrestamosPaymentRemoteResponse payment,
      BigDecimal interestCollected) {
    return duplicateDetector.detect(
        userId,
        profileId,
        cfg.getAccountId(),
        cfg.getInterestIncomeCategoryId(),
        PAYMENT_ENTITY,
        paymentId,
        PAYMENT_INTEREST_INCOME_EVENT,
        payment.fechaPago(),
        interestCollected);
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
    if (!accountRepository.existsByIdAndProfileId(cfg.getAccountId(), profileId)) {
      throw new BadRequestException("La cuenta configurada no pertenece al perfil");
    }
    validateCategory(profileId, cfg.getLoanDisbursementCategoryId());
    validateCategory(profileId, cfg.getPrincipalRecoveryCategoryId());
    validateCategory(profileId, cfg.getInterestIncomeCategoryId());
  }

  private void validateCategory(UUID profileId, UUID categoryId) {
    Category category =
        categoryRepository
            .findById(categoryId)
            .orElseThrow(() -> new BadRequestException("Categoría configurada inexistente"));
    if (category.getProfileId() != null && !category.getProfileId().equals(profileId)) {
      throw new BadRequestException("Categoría configurada no pertenece al perfil");
    }
  }

  private void ensureProfileBelongsToUser(UUID userId, UUID profileId) {
    if (!profileRepository.existsByIdAndUserId(profileId, userId)) {
      throw new ForbiddenException("Profile no pertenece al usuario");
    }
  }

  private ExternalIntegrationDiagnosticResponse diagnosticAndLog(
      String status,
      String message,
      UUID userId,
      UUID profileId,
      long startedAt,
      boolean remoteCheckExecuted,
      int createdMovements,
      int errors,
      boolean integrationEnabled,
      boolean syncEnabled,
      String baseUrl,
      String apiPrefix,
      boolean hasUsername,
      boolean hasPassword,
      List<String> missingFields) {
    log.info(
        "external-loans operation=health profileId={} userId={} remoteBaseUrl={} apiPrefix={} durationMs={} status={} createdMovements={} skippedDuplicates=0 errors={} integrationEnabled={} syncEnabled={} remoteCheckExecuted={}",
        profileId,
        userId,
        baseUrl,
        apiPrefix,
        System.currentTimeMillis() - startedAt,
        status,
        createdMovements,
        errors,
        integrationEnabled,
        syncEnabled,
        remoteCheckExecuted);
    return ExternalIntegrationDiagnosticResponse.of(
        status,
        message,
        integrationEnabled,
        syncEnabled,
        baseUrl,
        apiPrefix,
        hasUsername,
        hasPassword,
        properties.connectTimeoutMs(),
        properties.readTimeoutMs(),
        remoteCheckExecuted,
        missingFields);
  }

  private List<String> parseMissingFields() {
    String missing = properties.missingRequiredFields();
    if (!StringUtils.hasText(missing)) {
      return List.of();
    }
    return Arrays.stream(missing.split(",")).map(String::trim).filter(StringUtils::hasText).toList();
  }

  private String sanitizeUrl(String baseUrl) {
    if (!StringUtils.hasText(baseUrl)) {
      return "";
    }
    return baseUrl.replaceAll("(?i)://[^/@:]+:[^/@]+@", "://***:***@");
  }
}
