package com.hogaria.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.hogaria.service.CategoryKeyNormalizer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class FlywayBaselineMigrationIntegrityPostgresIT {

    private static final int EXPECTED_ACTIVE_GLOBAL_CATEGORY_COUNT = 172;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("hogaria_baseline");

    private static JdbcTemplate jdbc;

    @BeforeAll
    static void migrateEmptyDatabase() {
        var dataSource = new DriverManagerDataSource(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        );

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        jdbc = new JdbcTemplate(dataSource);
    }

    @Test
    void schemaContainsFinalTablesNullableReferencesAndKeyIndexes() {
        assertThat(publicTables()).containsAll(Set.of(
                "app_user",
                "financial_profile",
                "account",
                "category",
                "money_transaction",
                "budget_year",
                "budget_month",
                "budget_category_item",
                "financial_goal",
                "habit",
                "habit_checkin",
                "inflation_index",
                "excel_import_batch",
                "excel_import_row",
                "monthly_plan_item",
                "monthly_plan_transaction_match",
                "external_loan_sync_config",
                "external_sync_mapping",
                "transaction_import_reference"
        ));

        assertColumnNullable("money_transaction", "category_id");
        assertColumnNullable("external_loan_sync_config", "account_id");
        assertColumnNullable("external_loan_sync_config", "loan_disbursement_category_id");
        assertColumnNullable("external_loan_sync_config", "principal_recovery_category_id");
        assertColumnNullable("external_loan_sync_config", "interest_income_category_id");

        assertIndexExists("ux_category_profile_key_type");
        assertIndexExists("ux_category_global_key_type");
        assertIndexExists("ux_account_profile_key_currency_active");
        assertIndexExists("ux_money_tx_profile_source_operation_strict");
        assertIndexExists("ux_money_tx_profile_source_hash");
    }

    @Test
    void globalCategorySeedMatchesNormalizerAndHasNoActiveDuplicatesOrOrphans() {
        assertThat(count("""
                SELECT count(*)
                FROM category
                WHERE profile_id IS NULL
                  AND active = TRUE
                """))
                .isEqualTo(EXPECTED_ACTIVE_GLOBAL_CATEGORY_COUNT);

        assertThat(count("""
                SELECT count(*)
                FROM (
                    SELECT category_key, type
                    FROM category
                    WHERE profile_id IS NULL
                      AND active = TRUE
                    GROUP BY category_key, type
                    HAVING count(*) > 1
                ) duplicates
                """))
                .isZero();

        assertThat(count("""
                SELECT count(*)
                FROM (
                    SELECT name, type
                    FROM category
                    WHERE profile_id IS NULL
                      AND active = TRUE
                    GROUP BY name, type
                    HAVING count(*) > 1
                ) duplicates
                """))
                .isZero();

        assertThat(count("""
                SELECT count(*)
                FROM category child
                LEFT JOIN category parent ON parent.id = child.parent_id
                WHERE child.parent_id IS NOT NULL
                  AND parent.id IS NULL
                """))
                .isZero();

        var normalizer = new CategoryKeyNormalizer();
        List<Map.Entry<String, String>> seededKeys = jdbc.query("""
                SELECT name, category_key
                FROM category
                WHERE profile_id IS NULL
                  AND active = TRUE
                """, (rs, rowNum) -> Map.entry(rs.getString("name"), rs.getString("category_key")));

        for (var seededKey : seededKeys) {
            assertThat(seededKey.getValue())
                    .as("category_key for %s", seededKey.getKey())
                    .isEqualTo(normalizer.normalize(seededKey.getKey()));
        }
    }

    @Test
    void requiredRootAndCjPrestamosCategoriesExist() {
        assertRootCategoryExists("CJ - Ingresos de préstamos", "INCOME");
        assertRootCategoryExists("CJ - Préstamos otorgados", "INVESTMENT");

        assertGlobalCategoryExists("CJ - Interés cobrado", "INCOME");
        assertGlobalCategoryExists("CJ - Mora cobrada", "INCOME");
        assertGlobalCategoryExists("CJ - Comisión cobrada", "INCOME");
        assertGlobalCategoryExists("CJ - Capital prestado", "INVESTMENT");
        assertGlobalCategoryExists("CJ - Capital recuperado", "INVESTMENT");
        assertGlobalCategoryExists("CJ - Ajuste de préstamo", "INVESTMENT");
    }

    @Test
    void cjCapitalPrestadoUsesRecoverableOutflowAccountingSemantics() {
        var category = globalCategory("CJ - Capital prestado", "INVESTMENT");

        assertThat(category.get("category_key")).isEqualTo("cjcapitalprestado");
        assertThat(category.get("default_movement_type")).isEqualTo("ADJUSTMENT");
        assertThat(category.get("budgetable")).isEqualTo(false);
        assertThat(category.get("technical")).isEqualTo(false);
    }

    @Test
    void cjCapitalRecuperadoIsNotBudgetableBecausePrincipalRecoveryIsARecoveryFlow() {
        var category = globalCategory("CJ - Capital recuperado", "INVESTMENT");

        assertThat(category.get("category_key")).isEqualTo("cjcapitalrecuperado");
        assertThat(category.get("default_movement_type")).isEqualTo("SAVING");
        assertThat(category.get("budgetable")).isEqualTo(false);
        assertThat(category.get("technical")).isEqualTo(false);
    }

    @Test
    void cjPrestamosStrictIdempotencyIndexesExist() {
        assertIndexExists("ux_money_tx_profile_source_operation_strict");
        assertIndexExists("ux_money_tx_profile_source_hash");
    }

    @Test
    void flywayHistoryContainsOnlyTheSingleBaselineMigration() {
        assertThat(count("SELECT count(*) FROM flyway_schema_history WHERE success = TRUE"))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT script FROM flyway_schema_history WHERE success = TRUE",
                String.class
        ))
                .isEqualTo("V1__baseline_schema_and_seed.sql");
    }

    private static Set<String> publicTables() {
        return Set.copyOf(jdbc.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_type = 'BASE TABLE'
                """, String.class));
    }

    private static void assertColumnNullable(String tableName, String columnName) {
        assertThat(jdbc.queryForObject("""
                SELECT is_nullable
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = ?
                  AND column_name = ?
                """, String.class, tableName, columnName))
                .as("%s.%s should be nullable", tableName, columnName)
                .isEqualTo("YES");
    }

    private static void assertIndexExists(String indexName) {
        assertThat(count("""
                SELECT count(*)
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND indexname = ?
                """, indexName))
                .as("index %s should exist", indexName)
                .isEqualTo(1);
    }

    private static void assertRootCategoryExists(String name, String type) {
        assertThat(count("""
                SELECT count(*)
                FROM category
                WHERE profile_id IS NULL
                  AND parent_id IS NULL
                  AND active = TRUE
                  AND name = ?
                  AND type = ?
                """, name, type))
                .as("root category %s/%s should exist", name, type)
                .isEqualTo(1);
    }

    private static void assertGlobalCategoryExists(String name, String type) {
        assertThat(count("""
                SELECT count(*)
                FROM category
                WHERE profile_id IS NULL
                  AND active = TRUE
                  AND name = ?
                  AND type = ?
                """, name, type))
                .as("global category %s/%s should exist", name, type)
                .isEqualTo(1);
    }

    private static Map<String, Object> globalCategory(String name, String type) {
        return jdbc.queryForMap("""
                SELECT category_key, default_movement_type, budgetable, technical
                FROM category
                WHERE profile_id IS NULL
                  AND active = TRUE
                  AND name = ?
                  AND type = ?
                """, name, type);
    }

    private static int count(String sql, Object... args) {
        Integer result = jdbc.queryForObject(sql, Integer.class, args);
        return result == null ? 0 : result;
    }
}
