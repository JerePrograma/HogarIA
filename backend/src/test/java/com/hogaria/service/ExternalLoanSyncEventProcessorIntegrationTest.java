package com.hogaria.service;

import static org.junit.jupiter.api.Assertions.*;

import com.hogaria.entity.*;
import com.hogaria.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class ExternalLoanSyncEventProcessorIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @DynamicPropertySource
  static void configureDatasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    registry.add("spring.flyway.enabled", () -> "true");
  }

  private static final String SYSTEM = "CJPRESTAMOS";
  private static final String ENTITY_TYPE = "LOAN";
  private static final String EVENT_TYPE = "DISBURSEMENT";

  @Autowired private ExternalLoanSyncEventProcessor processor;
  @Autowired private AppUserRepository appUserRepository;
  @Autowired private FinancialProfileRepository financialProfileRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private CategoryRepository categoryRepository;
  @Autowired private ExternalLoanSyncConfigRepository syncConfigRepository;
  @Autowired private MoneyTransactionRepository moneyTransactionRepository;
  @Autowired private ExternalSyncMappingRepository externalSyncMappingRepository;

  private UUID userId;
  private UUID profileId;
  private ExternalLoanSyncConfig syncConfig;

  @BeforeEach
  void setUp() {
    externalSyncMappingRepository.deleteAll();
    moneyTransactionRepository.deleteAll();
    syncConfigRepository.deleteAll();
    categoryRepository.deleteAll();
    accountRepository.deleteAll();
    financialProfileRepository.deleteAll();
    appUserRepository.deleteAll();

    AppUser user =
        appUserRepository.save(
            AppUser.builder()
                .email("rollback-sync@test.com")
                .passwordHash("hash")
                .fullName("Rollback Sync")
                .build());
    userId = user.getId();

    FinancialProfile profile =
        financialProfileRepository.save(
            FinancialProfile.builder()
                .userId(userId)
                .name("Perfil test sync")
                .type(FinancialProfile.Type.PERSONAL)
                .baseCurrency("ARS")
                .activeYear(Year.now().getValue())
                .active(true)
                .build());
    profileId = profile.getId();

    Account account =
        accountRepository.save(
            Account.builder()
                .profileId(profileId)
                .name("Cuenta sync")
                .accountType(Account.AccountType.BANK)
                .currency("ARS")
                .active(true)
                .build());

    Category disbursementCategory =
        categoryRepository.save(
            Category.builder()
                .profileId(profileId)
                .name("Préstamos otorgados")
                .type(Category.Type.VARIABLE_EXPENSE)
                .scope(Category.Scope.PERSONAL)
                .active(true)
                .build());

    Category principalRecoveryCategory =
        categoryRepository.save(
            Category.builder()
                .profileId(profileId)
                .name("Recupero de capital")
                .type(Category.Type.INCOME)
                .scope(Category.Scope.PERSONAL)
                .active(true)
                .build());

    Category interestCategory =
        categoryRepository.save(
            Category.builder()
                .profileId(profileId)
                .name("Interés cobrado")
                .type(Category.Type.INCOME)
                .scope(Category.Scope.PERSONAL)
                .active(true)
                .build());

    syncConfig =
        syncConfigRepository.save(
            ExternalLoanSyncConfig.builder()
                .profileId(profileId)
                .accountId(account.getId())
                .loanDisbursementCategoryId(disbursementCategory.getId())
                .principalRecoveryCategoryId(principalRecoveryCategory.getId())
                .interestIncomeCategoryId(interestCategory.getId())
                .enabled(true)
                .build());
  }

  @Test
  void rollbackCuandoFallaMappingDespuesDeCrearMovimiento_noDebePersistirMovimientoNiMappingProcessed() {
    String loanIdQueRompePersistencia = "L".repeat(81);

    assertThrows(
        DataAccessException.class,
        () ->
            processor.processDisbursement(
                userId,
                profileId,
                syncConfig,
                loanIdQueRompePersistencia,
                "Cliente",
                LocalDate.of(2026, 5, 10),
                new BigDecimal("1000.00")));

    assertTrue(moneyTransactionRepository.findByProfileId(profileId).isEmpty());
    assertTrue(
        externalSyncMappingRepository
            .findByProfileIdAndExternalSystemAndExternalEntityTypeAndExternalEntityIdAndExternalEventType(
                profileId, SYSTEM, ENTITY_TYPE, loanIdQueRompePersistencia, EVENT_TYPE)
            .isEmpty());
  }

  @Test
  void reintentoDespuesDeRollback_creaUnaSolaVez() {
    String loanIdQueRompePersistencia = "L".repeat(81);
    assertThrows(
        DataAccessException.class,
        () ->
            processor.processDisbursement(
                userId,
                profileId,
                syncConfig,
                loanIdQueRompePersistencia,
                "Cliente",
                LocalDate.of(2026, 5, 10),
                new BigDecimal("1000.00")));

    String loanIdValido = "loan-rollback-retry-001";
    boolean procesado =
        processor.processDisbursement(
            userId,
            profileId,
            syncConfig,
            loanIdValido,
            "Cliente",
            LocalDate.of(2026, 5, 10),
            new BigDecimal("1000.00"));

    assertTrue(procesado);
    List<MoneyTransaction> transactions = moneyTransactionRepository.findByProfileId(profileId);
    assertEquals(1, transactions.size());

    ExternalSyncMapping mapping =
        externalSyncMappingRepository
            .findByProfileIdAndExternalSystemAndExternalEntityTypeAndExternalEntityIdAndExternalEventType(
                profileId, SYSTEM, ENTITY_TYPE, loanIdValido, EVENT_TYPE)
            .orElseThrow();

    assertEquals("PROCESSED", mapping.getStatus());
    assertEquals(transactions.get(0).getId(), mapping.getMoneyTransactionId());
  }

  @Test
  void reintentoConMappingProcessed_noDuplicaMovimiento() {
    String loanIdValido = "loan-idempotency-001";

    boolean first =
        processor.processDisbursement(
            userId,
            profileId,
            syncConfig,
            loanIdValido,
            "Cliente",
            LocalDate.of(2026, 5, 10),
            new BigDecimal("1000.00"));
    boolean second =
        processor.processDisbursement(
            userId,
            profileId,
            syncConfig,
            loanIdValido,
            "Cliente",
            LocalDate.of(2026, 5, 10),
            new BigDecimal("1000.00"));

    assertTrue(first);
    assertFalse(second);

    List<MoneyTransaction> transactions = moneyTransactionRepository.findByProfileId(profileId);
    assertEquals(1, transactions.size());

    ExternalSyncMapping mapping =
        externalSyncMappingRepository
            .findByProfileIdAndExternalSystemAndExternalEntityTypeAndExternalEntityIdAndExternalEventType(
                profileId, SYSTEM, ENTITY_TYPE, loanIdValido, EVENT_TYPE)
            .orElseThrow();

    assertEquals("PROCESSED", mapping.getStatus());
    assertEquals(transactions.get(0).getId(), mapping.getMoneyTransactionId());
  }
}
