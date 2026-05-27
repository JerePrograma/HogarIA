package com.hogaria.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hogaria.domains.transactions.lifecycle.TransactionDeletionObserver;
import com.hogaria.entity.Account;
import com.hogaria.entity.AppUser;
import com.hogaria.entity.Category;
import com.hogaria.entity.ExternalSyncMapping;
import com.hogaria.entity.FinancialProfile;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.entity.MonthlyPlanItem;
import com.hogaria.entity.MonthlyPlanTransactionMatch;
import com.hogaria.repository.AccountRepository;
import com.hogaria.repository.AppUserRepository;
import com.hogaria.repository.CategoryRepository;
import com.hogaria.repository.ExternalSyncMappingRepository;
import com.hogaria.repository.FinancialProfileRepository;
import com.hogaria.repository.MoneyTransactionRepository;
import com.hogaria.repository.MonthlyPlanItemRepository;
import com.hogaria.repository.MonthlyPlanTransactionMatchRepository;
import com.hogaria.service.MonthlyPlanService;
import com.hogaria.service.TransactionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class TransactionDeletionPostgresTest {

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

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private FinancialProfileRepository profileRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ExternalSyncMappingRepository externalSyncMappingRepository;
    @Autowired private MoneyTransactionRepository transactionRepository;
    @Autowired private MonthlyPlanItemRepository monthlyPlanItemRepository;
    @Autowired private MonthlyPlanTransactionMatchRepository matchRepository;
    @Autowired private MonthlyPlanService monthlyPlanService;
    @Autowired private TransactionService transactionService;

    @MockBean private TransactionDeletionObserver deletionObserver;

    private UUID userId;
    private UUID profileId;
    private UUID accountId;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        reset(deletionObserver);
        cleanupDatabase();

        var fixture = createFixture("main");
        userId = fixture.userId();
        profileId = fixture.profileId();
        accountId = fixture.accountId();
        categoryId = fixture.categoryId();
    }

    @Test
    void deleteManualTransactionWithoutLinksReturnsPhysicalDeleteResponseAndDeletesRow() throws Exception {
        var transaction = saveTransaction(MoneyTransaction.Origin.MANUAL);

        deleteTransaction(transaction.getId(), userId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(transaction.getId().toString()))
                .andExpect(jsonPath("$.mode").value("PHYSICAL_DELETE"))
                .andExpect(jsonPath("$.code").value("TRANSACTION_PHYSICALLY_DELETED"))
                .andExpect(jsonPath("$.message").value("Movimiento eliminado correctamente."))
                .andExpect(jsonPath("$.resultingStatus").doesNotExist())
                .andExpect(jsonPath("$.resultingClassificationStatus").doesNotExist());

        assertThat(transactionRepository.findById(transaction.getId())).isEmpty();
    }

    @Test
    void deleteTransactionLinkedByMonthlyPlanItemUnlinksItemBeforeDeleting() throws Exception {
        var transaction = saveTransaction(MoneyTransaction.Origin.MANUAL);
        var item = saveLinkedPlanItem(transaction.getId(), MonthlyPlanItem.Status.PAID);

        deleteTransaction(transaction.getId(), userId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("PHYSICAL_DELETE"))
                .andExpect(jsonPath("$.linkedItemsUpdated").value(1));

        assertThat(transactionRepository.findById(transaction.getId())).isEmpty();

        var reloadedItem = monthlyPlanItemRepository.findById(item.getId()).orElseThrow();
        assertThat(reloadedItem.getTransactionId()).isNull();
        assertThat(reloadedItem.getStatus()).isEqualTo(MonthlyPlanItem.Status.SCHEDULED);
    }

    @Test
    void deleteTransactionWithReconciliationMatchDeletesMatchBeforeDeleting() throws Exception {
        var transaction = saveTransaction(MoneyTransaction.Origin.MANUAL);
        var item = savePlanItem(MonthlyPlanItem.Status.ESTIMATED);
        saveMatch(item.getId(), transaction.getId(), MonthlyPlanTransactionMatch.MatchType.MANUAL);

        deleteTransaction(transaction.getId(), userId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("PHYSICAL_DELETE"))
                .andExpect(jsonPath("$.matchesDeleted").value(1));

        assertThat(transactionRepository.findById(transaction.getId())).isEmpty();
        assertThat(matchRepository.findByProfileIdAndMoneyTransactionId(profileId, transaction.getId())).isEmpty();
        assertThat(monthlyPlanItemRepository.findById(item.getId())).isPresent();
    }

    @Test
    void deleteTransactionConvertedFromMonthlyPlanItemRevertsPlanItemAndSystemMatch() throws Exception {
        var item = savePlanItem(MonthlyPlanItem.Status.ESTIMATED);
        var converted = monthlyPlanService.convert(userId, profileId, item.getId());

        assertThat(converted.transactionId()).isNotNull();
        assertThat(matchRepository.findByProfileIdAndMoneyTransactionId(profileId, converted.transactionId()))
                .hasSize(1);

        deleteTransaction(converted.transactionId(), userId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("PHYSICAL_DELETE"))
                .andExpect(jsonPath("$.systemConversionMatchesDeleted").value(1));

        assertThat(transactionRepository.findById(converted.transactionId())).isEmpty();
        assertThat(matchRepository.findByProfileIdAndMoneyTransactionId(profileId, converted.transactionId()))
                .isEmpty();

        var reloadedItem = monthlyPlanItemRepository.findById(item.getId()).orElseThrow();
        assertThat(reloadedItem.getTransactionId()).isNull();
        assertThat(reloadedItem.getStatus()).isEqualTo(MonthlyPlanItem.Status.SCHEDULED);
    }

    @Test
    void deleteImportedTransactionReturnsSoftIgnoreResponseAndKeepsRow() throws Exception {
        var transaction = saveTransaction(MoneyTransaction.Origin.IMPORT);

        deleteTransaction(transaction.getId(), userId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(transaction.getId().toString()))
                .andExpect(jsonPath("$.mode").value("SOFT_IGNORE"))
                .andExpect(jsonPath("$.code").value("IMPORTED_TRANSACTION_SOFT_IGNORED"))
                .andExpect(jsonPath("$.message").value("Movimiento ignorado para preservar trazabilidad."))
                .andExpect(jsonPath("$.resultingStatus").value("IGNORED"))
                .andExpect(jsonPath("$.resultingClassificationStatus").value("IGNORED_BY_RULE"));

        var reloaded = transactionRepository.findById(transaction.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(MoneyTransaction.Status.IGNORED);
        assertThat(reloaded.getClassificationStatus()).isEqualTo(MoneyTransaction.ClassificationStatus.IGNORED_BY_RULE);
        assertThat(reloaded.getClassificationReason()).isEqualTo("IMPORTED_TRANSACTION_SOFT_IGNORED");
    }

    @Test
    void deleteExternalSyncTransactionSoftIgnoresAndKeepsMapping() throws Exception {
        var transaction = saveTransaction(MoneyTransaction.Origin.SYSTEM);
        var mapping = saveExternalSyncMapping(transaction.getId());

        deleteTransaction(transaction.getId(), userId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("SOFT_IGNORE"))
                .andExpect(jsonPath("$.code").value("TRANSACTION_EXTERNAL_SYNC_SOFT_IGNORED"));

        var reloaded = transactionRepository.findById(transaction.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(MoneyTransaction.Status.IGNORED);
        assertThat(externalSyncMappingRepository.findById(mapping.getId())).isPresent();
    }

    @Test
    void deleteCjPrestamosTransactionSoftIgnores() throws Exception {
        var transaction = saveTransaction(MoneyTransaction.Origin.SYSTEM, "CJPRESTAMOS");

        deleteTransaction(transaction.getId(), userId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("SOFT_IGNORE"))
                .andExpect(jsonPath("$.code").value("EXTERNAL_LOAN_TRANSACTION_SOFT_IGNORED"));

        var reloaded = transactionRepository.findById(transaction.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(MoneyTransaction.Status.IGNORED);
        assertThat(reloaded.getClassificationReason()).isEqualTo("EXTERNAL_LOAN_TRANSACTION_SOFT_IGNORED");
    }

    @Test
    void softIgnoreLinkedImportedTransactionUnlinksMonthlyPlan() throws Exception {
        var transaction = saveTransaction(MoneyTransaction.Origin.IMPORT);
        var item = saveLinkedPlanItem(transaction.getId(), MonthlyPlanItem.Status.COLLECTED);
        saveMatch(item.getId(), transaction.getId(), MonthlyPlanTransactionMatch.MatchType.MANUAL);

        deleteTransaction(transaction.getId(), userId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("SOFT_IGNORE"))
                .andExpect(jsonPath("$.linkedItemsUpdated").value(1))
                .andExpect(jsonPath("$.matchesDeleted").value(1));

        var reloadedTransaction = transactionRepository.findById(transaction.getId()).orElseThrow();
        assertThat(reloadedTransaction.getStatus()).isEqualTo(MoneyTransaction.Status.IGNORED);

        var reloadedItem = monthlyPlanItemRepository.findById(item.getId()).orElseThrow();
        assertThat(reloadedItem.getTransactionId()).isNull();
        assertThat(reloadedItem.getStatus()).isEqualTo(MonthlyPlanItem.Status.SCHEDULED);
        assertThat(matchRepository.findByProfileIdAndMoneyTransactionId(profileId, transaction.getId())).isEmpty();
    }

    @Test
    void physicalDeleteResponseIncludesUnlinkCounters() throws Exception {
        var transaction = saveTransaction(MoneyTransaction.Origin.MANUAL);
        var item = saveLinkedPlanItem(transaction.getId(), MonthlyPlanItem.Status.PAID);
        saveMatch(item.getId(), transaction.getId(), MonthlyPlanTransactionMatch.MatchType.MANUAL);

        deleteTransaction(transaction.getId(), userId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("PHYSICAL_DELETE"))
                .andExpect(jsonPath("$.linkedItemsUpdated").value(1))
                .andExpect(jsonPath("$.matchesDeleted").value(1));
    }

    @Test
    void deleteTransactionFromAnotherProfileReturnsForbidden() throws Exception {
        var otherFixture = createFixture("other");
        var transaction = saveTransaction(
                otherFixture.profileId(),
                otherFixture.accountId(),
                otherFixture.categoryId(),
                MoneyTransaction.Origin.MANUAL
        );

        deleteTransaction(transaction.getId(), userId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        assertThat(transactionRepository.findById(transaction.getId())).isPresent();
    }

    @Test
    void deleteMissingTransactionReturnsNotFound() throws Exception {
        deleteTransaction(UUID.randomUUID(), userId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void deleteRollbackRestoresPlanLinksWhenFailureHappensAfterUnlink() {
        var transaction = saveTransaction(MoneyTransaction.Origin.MANUAL);
        var item = saveLinkedPlanItem(transaction.getId(), MonthlyPlanItem.Status.PAID);
        saveMatch(item.getId(), transaction.getId(), MonthlyPlanTransactionMatch.MatchType.SYSTEM_CONVERSION);

        doThrow(new IllegalStateException("simulated failure"))
                .when(deletionObserver)
                .beforeTransactionRemoval(any(), any(), any());

        assertThatThrownBy(() -> transactionService.delete(userId, transaction.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("simulated failure");

        assertThat(transactionRepository.findById(transaction.getId())).isPresent();

        var reloadedItem = monthlyPlanItemRepository.findById(item.getId()).orElseThrow();
        assertThat(reloadedItem.getTransactionId()).isEqualTo(transaction.getId());
        assertThat(reloadedItem.getStatus()).isEqualTo(MonthlyPlanItem.Status.PAID);

        assertThat(matchRepository.findByProfileIdAndMoneyTransactionId(profileId, transaction.getId()))
                .hasSize(1);
    }

    private org.springframework.test.web.servlet.ResultActions deleteTransaction(
            UUID transactionId,
            UUID authenticatedUserId
    ) throws Exception {
        return mockMvc.perform(
                delete("/api/transactions/{id}", transactionId)
                        .with(authenticatedUser(authenticatedUserId))
                        .header("X-User-Id", authenticatedUserId)
        );
    }

    private RequestPostProcessor authenticatedUser(UUID authenticatedUserId) {
        return authentication(new UsernamePasswordAuthenticationToken(
                authenticatedUserId,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        ));
    }

    private MoneyTransaction saveTransaction(MoneyTransaction.Origin origin) {
        return saveTransaction(origin, null);
    }

    private MoneyTransaction saveTransaction(MoneyTransaction.Origin origin, String source) {
        return saveTransaction(profileId, accountId, categoryId, origin, source);
    }

    private MoneyTransaction saveTransaction(
            UUID targetProfileId,
            UUID targetAccountId,
            UUID targetCategoryId,
            MoneyTransaction.Origin origin
    ) {
        return saveTransaction(targetProfileId, targetAccountId, targetCategoryId, origin, null);
    }

    private MoneyTransaction saveTransaction(
            UUID targetProfileId,
            UUID targetAccountId,
            UUID targetCategoryId,
            MoneyTransaction.Origin origin,
            String source
    ) {
        return transactionRepository.saveAndFlush(MoneyTransaction.builder()
                .profileId(targetProfileId)
                .accountId(targetAccountId)
                .categoryId(targetCategoryId)
                .movementType(MoneyTransaction.MovementType.EXPENSE)
                .realDate(LocalDate.now())
                .budgetDate(LocalDate.now().withDayOfMonth(1))
                .amount(new BigDecimal("100.00"))
                .currency("ARS")
                .description("Movimiento de prueba")
                .origin(origin)
                .status(MoneyTransaction.Status.CONFIRMED)
                .source(source)
                .build());
    }

    private MonthlyPlanItem saveLinkedPlanItem(UUID transactionId, MonthlyPlanItem.Status status) {
        var item = savePlanItem(status);
        item.setTransactionId(transactionId);
        return monthlyPlanItemRepository.saveAndFlush(item);
    }

    private MonthlyPlanItem savePlanItem(MonthlyPlanItem.Status status) {
        var expectedDate = LocalDate.now().plusDays(3);

        return monthlyPlanItemRepository.saveAndFlush(MonthlyPlanItem.builder()
                .profileId(profileId)
                .accountId(accountId)
                .categoryId(categoryId)
                .type(MonthlyPlanItem.Type.EXPENSE)
                .title("Pago planificado")
                .expectedDate(expectedDate)
                .periodYear(expectedDate.getYear())
                .periodMonth(expectedDate.getMonthValue())
                .amount(new BigDecimal("100.00"))
                .currency("ARS")
                .status(status)
                .source(MonthlyPlanItem.Source.MANUAL)
                .build());
    }

    private MonthlyPlanTransactionMatch saveMatch(
            UUID itemId,
            UUID transactionId,
            MonthlyPlanTransactionMatch.MatchType matchType
    ) {
        return matchRepository.saveAndFlush(MonthlyPlanTransactionMatch.builder()
                .profileId(profileId)
                .monthlyPlanItemId(itemId)
                .moneyTransactionId(transactionId)
                .matchedAmount(new BigDecimal("100.00"))
                .matchType(matchType)
                .confidence(MonthlyPlanTransactionMatch.Confidence.HIGH)
                .build());
    }

    private ExternalSyncMapping saveExternalSyncMapping(UUID transactionId) {
        return externalSyncMappingRepository.saveAndFlush(ExternalSyncMapping.builder()
                .profileId(profileId)
                .externalSystem("CJPRESTAMOS")
                .externalEntityType("LOAN")
                .externalEntityId(UUID.randomUUID().toString())
                .externalEventType("DISBURSEMENT")
                .moneyTransactionId(transactionId)
                .eventHash(UUID.randomUUID().toString())
                .status("PROCESSED")
                .build());
    }

    private Fixture createFixture(String suffix) {
        var user = appUserRepository.saveAndFlush(AppUser.builder()
                .email("tx-delete-" + suffix + "-" + UUID.randomUUID() + "@test.com")
                .passwordHash("hash")
                .fullName("Transaction Delete")
                .build());

        var profile = profileRepository.saveAndFlush(FinancialProfile.builder()
                .userId(user.getId())
                .name("Perfil " + suffix)
                .type(FinancialProfile.Type.PERSONAL)
                .baseCurrency("ARS")
                .activeYear(Year.now().getValue())
                .active(true)
                .build());

        var account = accountRepository.saveAndFlush(Account.builder()
                .profileId(profile.getId())
                .name("Cuenta " + suffix)
                .accountType(Account.AccountType.BANK)
                .currency("ARS")
                .active(true)
                .build());

        var category = categoryRepository.saveAndFlush(Category.builder()
                .profileId(profile.getId())
                .name("Gastos " + suffix)
                .type(Category.Type.VARIABLE_EXPENSE)
                .scope(Category.Scope.PERSONAL)
                .active(true)
                .build());

        return new Fixture(user.getId(), profile.getId(), account.getId(), category.getId());
    }

    private void cleanupDatabase() {
        jdbcTemplate.update("DELETE FROM monthly_plan_transaction_match");
        jdbcTemplate.update("DELETE FROM external_sync_mapping");
        jdbcTemplate.update("DELETE FROM transaction_import_reference");
        jdbcTemplate.update("DELETE FROM monthly_plan_item");
        jdbcTemplate.update("DELETE FROM money_transaction");
        jdbcTemplate.update("DELETE FROM external_loan_sync_config");
        jdbcTemplate.update("DELETE FROM excel_import_row");
        jdbcTemplate.update("DELETE FROM excel_import_batch");
        jdbcTemplate.update("DELETE FROM budget_category_item");
        jdbcTemplate.update("DELETE FROM budget_month");
        jdbcTemplate.update("DELETE FROM budget_year");
        jdbcTemplate.update("DELETE FROM habit_checkin");
        jdbcTemplate.update("DELETE FROM habit");
        jdbcTemplate.update("DELETE FROM financial_goal");
        jdbcTemplate.update("DELETE FROM category WHERE profile_id IS NOT NULL");
        jdbcTemplate.update("DELETE FROM account");
        jdbcTemplate.update("DELETE FROM financial_profile");
        jdbcTemplate.update("DELETE FROM app_user WHERE email LIKE 'tx-delete-%'");
    }

    private record Fixture(UUID userId, UUID profileId, UUID accountId, UUID categoryId) {
    }
}
