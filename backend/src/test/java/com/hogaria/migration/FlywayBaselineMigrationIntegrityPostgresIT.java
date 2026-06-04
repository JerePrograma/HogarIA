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

    private static final int EXPECTED_ACTIVE_GLOBAL_CATEGORY_COUNT = 56;
    private static final int EXPECTED_GLOBAL_MERCHANT_ALIAS_COUNT = 15;

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
                "merchant_alias",
                "counterparty_alias",
                "classification_rule",
                "transaction_classification_audit",
                "monthly_plan_item",
                "monthly_plan_transaction_match",
                "external_loan_sync_config",
                "external_sync_mapping",
                "transaction_import_reference"
        ));

        assertColumnNullable("money_transaction", "category_id");
        assertColumnExists("money_transaction", "classification_explanation_json");
        assertColumnExists("excel_import_row", "raw_json");
        assertColumnExists("excel_import_row", "merchant_name");
        assertColumnExists("excel_import_row", "extended_description");
        assertColumnExists("excel_import_row", "normalized_description");
        assertColumnExists("excel_import_row", "source_hash");
        assertColumnExists("excel_import_row", "counterparty_document_hash");
        assertColumnExists("excel_import_row", "classification_explanation_json");
        assertColumnExists("transaction_import_reference", "classification_explanation_json");
        assertColumnNullable("external_loan_sync_config", "account_id");
        assertColumnNullable("external_loan_sync_config", "loan_disbursement_category_id");
        assertColumnNullable("external_loan_sync_config", "principal_recovery_category_id");
        assertColumnNullable("external_loan_sync_config", "interest_income_category_id");

        assertIndexExists("ux_category_profile_key_type");
        assertIndexExists("ux_category_global_key_type");
        assertIndexExists("ux_account_profile_key_currency_active");
        assertIndexExists("ux_money_tx_profile_source_hash");
        assertIndexExists("ux_money_tx_profile_source_operation_idempotent");
        assertIndexExists("idx_transaction_import_reference_profile_source_hash");
        assertIndexExists("ux_merchant_alias_global_source_alias");
        assertIndexExists("ux_merchant_alias_profile_source_alias");
        assertIndexExists("ux_counterparty_alias_profile_source_document");
        assertIndexExists("ux_classification_rule_profile_reason");

        assertIndexDoesNotExist("ux_money_tx_profile_source_operation_strict");
        assertIndexDefinitionContains("ux_money_tx_profile_source_hash", "status <> 'IGNORED'");
        assertIndexDefinitionContains("ux_money_tx_profile_source_operation_idempotent", "classification_status <> 'IGNORED_BY_RULE'");
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
    void requiredCategoryTreeExistsWithParentLinks() {
        for (var root : List.of(
                "Ingresos",
                "Alimentación",
                "Transporte",
                "Servicios",
                "Impuestos",
                "Seguros y mutuales",
                "Bancos y comisiones",
                "Deudas",
                "Transferencias",
                "Familia y personales",
                "Operaciones técnicas"
        )) {
            assertRootCategoryExists(root);
        }

        assertChildCategoryExists("Ingresos", "Sueldo");
        assertChildCategoryExists("Ingresos", "Intereses y rendimientos");
        assertChildCategoryExists("Ingresos", "Rendimiento Mercado Pago");
        assertChildCategoryExists("Alimentación", "Supermercado");
        assertChildCategoryExists("Alimentación", "Delivery y restaurantes");
        assertChildCategoryExists("Transporte", "Transporte público");
        assertChildCategoryExists("Transporte", "Taxi y apps");
        assertChildCategoryExists("Servicios", "Telefonía móvil");
        assertChildCategoryExists("Servicios", "Meli+");
        assertChildCategoryExists("Impuestos", "Monotributo");
        assertChildCategoryExists("Impuestos", "Percepciones RG 4815");
        assertChildCategoryExists("Deudas", "Préstamo Banco Provincia");
        assertChildCategoryExists("Deudas", "Mercado Crédito");
        assertChildCategoryExists("Transferencias", "Fondeo Mercado Pago");
        assertChildCategoryExists("Transferencias", "Cuenta DNI / DEBIN");
        assertChildCategoryExists("Operaciones técnicas", "Movimiento ignorado");
    }

    @Test
    void globalAliasesAndClassificationRulesAreSeededWithoutPrivateCounterparties() {
        assertThat(count("""
                SELECT count(*)
                FROM merchant_alias
                WHERE profile_id IS NULL
                  AND active = TRUE
                """))
                .isEqualTo(EXPECTED_GLOBAL_MERCHANT_ALIAS_COUNT);

        for (var alias : List.of(
                "PAYU*AR*UBER",
                "MERPAGO*SUBE",
                "BUSPLUS",
                "DIA TIENDA",
                "PEDIDOSYA",
                "MERPAGO*TUENTI",
                "EDEA",
                "CAMUZZI",
                "SHELL BOX",
                "MELI+",
                "MERCADO CREDITO",
                "MERCADOCRÉDITO",
                "EMOVA"
        )) {
            assertMerchantAliasExists(alias);
        }

        assertThat(count("SELECT count(*) FROM counterparty_alias")).isZero();
        assertClassificationRuleExists("RULE_DEBIT_CARD_PURCHASE_REVIEW", "REVIEW");
        assertClassificationRuleExists("RULE_DIRECT_DEBIT_REVIEW", "REVIEW");
        assertClassificationRuleExists("RULE_ARCA_AFIP_MONOTRIBUTO", "CLASSIFIED");
        assertClassificationRuleExists("RULE_MP_YIELD_EMPTY_DETAIL", "CLASSIFIED");
        assertClassificationRuleExists("RULE_MP_PAYMENT_LINK_REVIEW", "REVIEW");
    }

    @Test
    void schemaDoesNotContainDuplicateEquivalentIndexes() {
        assertThat(count("""
                SELECT count(*)
                FROM (
                    SELECT
                        indrelid,
                        indkey::text,
                        coalesce(pg_get_expr(indexprs, indrelid), '') AS indexprs,
                        coalesce(pg_get_expr(indpred, indrelid), '') AS indpred,
                        indisunique
                    FROM pg_index
                    JOIN pg_class idx ON idx.oid = indexrelid
                    JOIN pg_namespace ns ON ns.oid = idx.relnamespace
                    WHERE ns.nspname = 'public'
                    GROUP BY indrelid, indkey::text, coalesce(pg_get_expr(indexprs, indrelid), ''), coalesce(pg_get_expr(indpred, indrelid), ''), indisunique
                    HAVING count(*) > 1
                ) duplicates
                """))
                .isZero();
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

    private static void assertColumnExists(String tableName, String columnName) {
        assertThat(count("""
                SELECT count(*)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = ?
                  AND column_name = ?
                """, tableName, columnName))
                .as("%s.%s should exist", tableName, columnName)
                .isEqualTo(1);
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

    private static void assertIndexDoesNotExist(String indexName) {
        assertThat(count("""
                SELECT count(*)
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND indexname = ?
                """, indexName))
                .as("index %s should not exist", indexName)
                .isZero();
    }

    private static void assertIndexDefinitionContains(String indexName, String expected) {
        assertThat(jdbc.queryForObject("""
                SELECT indexdef
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND indexname = ?
                """, String.class, indexName))
                .contains(expected);
    }

    private static void assertRootCategoryExists(String name) {
        assertThat(count("""
                SELECT count(*)
                FROM category
                WHERE profile_id IS NULL
                  AND parent_id IS NULL
                  AND active = TRUE
                  AND name = ?
                """, name))
                .as("root category %s should exist", name)
                .isEqualTo(1);
    }

    private static void assertChildCategoryExists(String parentName, String childName) {
        assertThat(count("""
                SELECT count(*)
                FROM category child
                JOIN category parent ON parent.id = child.parent_id
                WHERE child.profile_id IS NULL
                  AND child.active = TRUE
                  AND parent.profile_id IS NULL
                  AND parent.active = TRUE
                  AND parent.name = ?
                  AND child.name = ?
                """, parentName, childName))
                .as("child category %s / %s should exist", parentName, childName)
                .isEqualTo(1);
    }

    private static void assertMerchantAliasExists(String aliasRaw) {
        assertThat(count("""
                SELECT count(*)
                FROM merchant_alias
                WHERE profile_id IS NULL
                  AND active = TRUE
                  AND alias_raw = ?
                """, aliasRaw))
                .as("global merchant alias %s should exist", aliasRaw)
                .isEqualTo(1);
    }

    private static void assertClassificationRuleExists(String reasonCode, String status) {
        assertThat(count("""
                SELECT count(*)
                FROM classification_rule
                WHERE profile_id IS NULL
                  AND active = TRUE
                  AND reason_code = ?
                  AND classification_status = ?
                """, reasonCode, status))
                .as("classification rule %s/%s should exist", reasonCode, status)
                .isEqualTo(1);
    }

    private static int count(String sql, Object... args) {
        Integer result = jdbc.queryForObject(sql, Integer.class, args);
        return result == null ? 0 : result;
    }
}
