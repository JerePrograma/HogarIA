package com.hogaria.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hogaria.entity.*;
import com.hogaria.integration.cjprestamos.CjPrestamosClient;
import com.hogaria.integration.cjprestamos.remote.CjPrestamosCashControlRemoteResponse;
import com.hogaria.integration.cjprestamos.remote.CjPrestamosDashboardRemoteResponse;
import com.hogaria.integration.cjprestamos.remote.CjPrestamosLoanActiveRemoteResponse;
import com.hogaria.integration.cjprestamos.remote.CjPrestamosPaymentRemoteResponse;
import com.hogaria.repository.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    properties = {
      "CJP_INTEGRATION_ENABLED=true",
      "CJP_SYNC_ENABLED=false",
      "CJP_BASE_URL=http://cjprestamos.test",
      "CJP_API_PREFIX=/api/v1/integration/hogaria",
      "CJP_USERNAME=test_user",
      "CJP_PASSWORD=test_pwd"
    })
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class ExternalLoansSyncSafetyPostgresIT {

  @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @DynamicPropertySource
  static void configureDatasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    registry.add("spring.flyway.enabled", () -> "true");
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private DataSource dataSource;
  @Autowired private AppUserRepository appUserRepository;
  @Autowired private FinancialProfileRepository financialProfileRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private CategoryRepository categoryRepository;
  @Autowired private ExternalLoanSyncConfigRepository syncConfigRepository;
  @Autowired private MoneyTransactionRepository moneyTransactionRepository;
  @Autowired private ExternalSyncMappingRepository externalSyncMappingRepository;

  @MockBean private CjPrestamosClient client;

  private UUID userId;
  private UUID profileId;

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
            AppUser.builder().email("sync-safety@test.com").passwordHash("hash").fullName("Sync").build());
    userId = user.getId();

    FinancialProfile profile =
        financialProfileRepository.save(
            FinancialProfile.builder()
                .userId(userId)
                .name("Perfil sync")
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
                .name("Cuenta")
                .accountType(Account.AccountType.BANK)
                .currency("ARS")
                .active(true)
                .build());

    Category disbursementCategory =
        categoryRepository.save(
            Category.builder()
                .profileId(profileId)
                .name("Préstamos")
                .type(Category.Type.VARIABLE_EXPENSE)
                .scope(Category.Scope.PERSONAL)
                .active(true)
                .build());

    Category principalRecoveryCategory =
        categoryRepository.save(
            Category.builder()
                .profileId(profileId)
                .name("Recupero")
                .type(Category.Type.INCOME)
                .scope(Category.Scope.PERSONAL)
                .active(true)
                .build());

    Category interestCategory =
        categoryRepository.save(
            Category.builder()
                .profileId(profileId)
                .name("Interés")
                .type(Category.Type.INCOME)
                .scope(Category.Scope.PERSONAL)
                .active(true)
                .build());

    syncConfigRepository.save(
        ExternalLoanSyncConfig.builder()
            .profileId(profileId)
            .enabled(true)
            .accountId(account.getId())
            .loanDisbursementCategoryId(disbursementCategory.getId())
            .principalRecoveryCategoryId(principalRecoveryCategory.getId())
            .interestIncomeCategoryId(interestCategory.getId())
            .build());

    when(client.getDashboardSummary(eq(profileId), eq(userId)))
        .thenReturn(
            new CjPrestamosDashboardRemoteResponse(
                BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, 1L));
    when(client.getCashControl(eq(profileId), eq(userId)))
        .thenReturn(
            new CjPrestamosCashControlRemoteResponse(
                BigDecimal.TEN,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0L,
                0L,
                BigDecimal.ZERO,
                BigDecimal.ZERO));
    when(client.getActiveLoans(eq(profileId), eq(userId)))
        .thenReturn(
            List.of(
                new CjPrestamosLoanActiveRemoteResponse(
                    2L,
                    10L,
                    "Cliente",
                    new BigDecimal("1000.00"),
                    10,
                    "MENSUAL",
                    "ACTIVO",
                    BigDecimal.ZERO,
                    new BigDecimal("1000.00"),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    LocalDateTime.now().minusDays(1),
                    LocalDateTime.now())));
    when(client.getLoanPayments(eq(profileId), eq(userId), any()))
        .thenReturn(
            List.of(
                new CjPrestamosPaymentRemoteResponse(
                    5L,
                    2L,
                    LocalDate.now(),
                    new BigDecimal("100.00"),
                    new BigDecimal("100.00"),
                    BigDecimal.ZERO,
                    null,
                    null,
                    "APLICADO")));
  }

  @Test
  void syncBloqueadoPorReadOnly_devuelve400SinWrites() throws Exception {
    mockMvc
        .perform(post("/api/profiles/{profileId}/external-loans/sync", profileId).header("X-User-Id", userId))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value(
                    "La sincronización contable está deshabilitada. La integración está en modo solo lectura."));

    org.assertj.core.api.Assertions.assertThat(moneyTransactionRepository.count()).isZero();
    org.assertj.core.api.Assertions.assertThat(externalSyncMappingRepository.count()).isZero();
  }

  @Test
  void dryRunPlanificaSinPersistir() throws Exception {
    mockMvc
        .perform(post("/api/profiles/{profileId}/external-loans/sync/dry-run", profileId).header("X-User-Id", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dryRun").value(true))
        .andExpect(jsonPath("$.movementsCreated").value(2))
        .andExpect(jsonPath("$.plannedMovements.length()").value(2));

    org.assertj.core.api.Assertions.assertThat(moneyTransactionRepository.count()).isZero();
    org.assertj.core.api.Assertions.assertThat(externalSyncMappingRepository.count()).isZero();
  }

  @Test
  void uniqueConstraintExisteConProfileEnExternalSyncMapping() throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      var metadata = connection.getMetaData();
      try (var rs = metadata.getIndexInfo(null, null, "external_sync_mapping", true, false)) {
        boolean found = false;
        while (rs.next()) {
          String indexName = rs.getString("INDEX_NAME");
          if ("uk_external_sync_mapping_external_event_profile".equalsIgnoreCase(indexName)) {
            found = true;
            break;
          }
        }
        org.assertj.core.api.Assertions.assertThat(found).isTrue();
      }
    }
  }
}
