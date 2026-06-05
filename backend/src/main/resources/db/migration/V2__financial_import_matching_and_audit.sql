ALTER TABLE money_transaction
    ADD COLUMN counterparty_document_hash VARCHAR(64),
    ADD COLUMN external_sequence VARCHAR(120);

ALTER TABLE money_transaction
    DROP CONSTRAINT ck_money_transaction_balance_impact;

ALTER TABLE money_transaction
    ADD CONSTRAINT ck_money_transaction_balance_impact CHECK (
        balance_impact IN (
            'OPERATING_INCOME',
            'CONSUMPTION_EXPENSE',
            'SAVING_OUTFLOW',
            'INVESTMENT_OUTFLOW',
            'DEBT_OUTFLOW',
            'RECOVERABLE_OUTFLOW',
            'PRINCIPAL_RECOVERY',
            'INTEREST_INCOME',
            'REFUND_OR_REIMBURSEMENT',
            'INTERNAL_TRANSFER',
            'EXTERNAL_TRANSFER',
            'CASH_WITHDRAWAL',
            'LOAN_ORIGINATION',
            'NEUTRAL_ADJUSTMENT',
            'IGNORED',
            'TECHNICAL',
            'UNKNOWN'
        )
    );

DROP INDEX ux_money_tx_profile_source_operation_idempotent;

CREATE UNIQUE INDEX ux_money_tx_profile_source_operation_signature
    ON money_transaction(profile_id, source, source_operation_id, real_date, amount, movement_type)
    WHERE source IS NOT NULL
      AND source_operation_id IS NOT NULL
      AND status <> 'IGNORED'
      AND classification_status <> 'IGNORED_BY_RULE';

CREATE INDEX idx_money_tx_counterparty_document_hash
    ON money_transaction(profile_id, counterparty_document_hash)
    WHERE counterparty_document_hash IS NOT NULL;

ALTER TABLE excel_import_row
    ADD COLUMN source VARCHAR(40),
    ADD COLUMN operation_datetime TIMESTAMP,
    ADD COLUMN operation_datetime_precision VARCHAR(20),
    ADD COLUMN signed_amount NUMERIC(19,2),
    ADD COLUMN amount_abs NUMERIC(19,2),
    ADD COLUMN counterparty VARCHAR(255),
    ADD COLUMN balance_impact VARCHAR(40),
    ADD COLUMN classification_layer VARCHAR(40),
    ADD COLUMN classification_matched_field VARCHAR(80),
    ADD COLUMN classification_matched_value VARCHAR(500),
    ADD COLUMN suggested_category_id UUID,
    ADD COLUMN suggested_category_name VARCHAR(255),
    ADD COLUMN warning VARCHAR(1000);

UPDATE excel_import_row row
SET source = batch.source,
    operation_datetime = COALESCE(row.operation_datetime, row.real_date::timestamp),
    operation_datetime_precision = COALESCE(row.operation_datetime_precision, 'DATE_ONLY'),
    amount_abs = COALESCE(row.amount_abs, row.amount),
    signed_amount = COALESCE(
        row.signed_amount,
        CASE
            WHEN row.movement_type IN ('EXPENSE', 'SAVING') THEN -row.amount
            ELSE row.amount
        END
    ),
    counterparty = COALESCE(row.counterparty, row.counterparty_name),
    warning = COALESCE(row.warning, row.error_message)
FROM excel_import_batch batch
WHERE batch.id = row.batch_id;

UPDATE excel_import_row
SET status = 'REVIEW'
WHERE status = 'WARNING';

ALTER TABLE excel_import_row
    DROP CONSTRAINT ck_excel_import_row_status;

ALTER TABLE excel_import_row
    ADD CONSTRAINT ck_excel_import_row_status CHECK (
        status IN (
            'READY',
            'NEEDS_CATEGORY',
            'REVIEW',
            'DUPLICATE',
            'DUPLICATE_EXACT',
            'POSSIBLE_INTERNAL_TRANSFER',
            'INTERNAL_TRANSFER_MATCHED',
            'POSSIBLE_CROSS_SOURCE_DUPLICATE',
            'IMPORTED',
            'SKIPPED',
            'ERROR'
        )
    ),
    ADD CONSTRAINT ck_excel_import_row_operation_datetime_precision CHECK (
        operation_datetime_precision IS NULL
        OR operation_datetime_precision IN ('DATE_ONLY', 'DATE_TIME')
    ),
    ADD CONSTRAINT ck_excel_import_row_balance_impact CHECK (
        balance_impact IS NULL
        OR balance_impact IN (
            'OPERATING_INCOME',
            'CONSUMPTION_EXPENSE',
            'SAVING_OUTFLOW',
            'INVESTMENT_OUTFLOW',
            'DEBT_OUTFLOW',
            'RECOVERABLE_OUTFLOW',
            'PRINCIPAL_RECOVERY',
            'INTEREST_INCOME',
            'REFUND_OR_REIMBURSEMENT',
            'INTERNAL_TRANSFER',
            'EXTERNAL_TRANSFER',
            'CASH_WITHDRAWAL',
            'LOAN_ORIGINATION',
            'NEUTRAL_ADJUSTMENT',
            'IGNORED',
            'TECHNICAL',
            'UNKNOWN'
        )
    ),
    ADD CONSTRAINT fk_excel_import_row_suggested_category
        FOREIGN KEY (suggested_category_id) REFERENCES category(id);

CREATE INDEX idx_excel_import_row_source_operation_date_amount
    ON excel_import_row(source, operation_datetime, amount_abs);

CREATE INDEX idx_excel_import_row_balance_impact
    ON excel_import_row(balance_impact);

CREATE TABLE import_counterparty_alias (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL,
    alias_type VARCHAR(40) NOT NULL,
    identifier VARCHAR(255) NOT NULL,
    identifier_normalized VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_import_counterparty_alias_profile
        FOREIGN KEY (profile_id) REFERENCES financial_profile(id) ON DELETE CASCADE,
    CONSTRAINT ck_import_counterparty_alias_type CHECK (
        alias_type IN ('OWN_ID', 'OWN_NAME', 'OWN_ACCOUNT', 'INTERNAL_ACCOUNT', 'TRUSTED_EXTERNAL')
    )
);

CREATE UNIQUE INDEX ux_import_counterparty_alias_profile_type_identifier
    ON import_counterparty_alias(profile_id, alias_type, identifier_normalized)
    WHERE active = TRUE;

CREATE INDEX idx_import_counterparty_alias_profile_active
    ON import_counterparty_alias(profile_id, active);

INSERT INTO import_counterparty_alias (
    profile_id,
    alias_type,
    identifier,
    identifier_normalized,
    display_name,
    notes
)
SELECT
    profile.id,
    seed.alias_type,
    seed.identifier,
    seed.identifier_normalized,
    'JEREMIAS JOSE RIVELL',
    'Seed local V1; configurable por perfil.'
FROM financial_profile profile
CROSS JOIN (
    VALUES
        ('OWN_ID', '20384307400', '20384307400'),
        ('OWN_NAME', 'JEREMIAS JOSE RIVELL', 'jeremias jose rivell')
) AS seed(alias_type, identifier, identifier_normalized)
ON CONFLICT DO NOTHING;

CREATE TABLE import_internal_transfer_match (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL,
    debit_import_row_id UUID NOT NULL,
    credit_import_row_id UUID NOT NULL,
    debit_transaction_id UUID,
    credit_transaction_id UUID,
    amount NUMERIC(19,2) NOT NULL,
    operation_date DATE NOT NULL,
    confidence NUMERIC(5,2) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_import_internal_transfer_match_profile
        FOREIGN KEY (profile_id) REFERENCES financial_profile(id) ON DELETE CASCADE,
    CONSTRAINT fk_import_internal_transfer_match_debit_row
        FOREIGN KEY (debit_import_row_id) REFERENCES excel_import_row(id) ON DELETE CASCADE,
    CONSTRAINT fk_import_internal_transfer_match_credit_row
        FOREIGN KEY (credit_import_row_id) REFERENCES excel_import_row(id) ON DELETE CASCADE,
    CONSTRAINT fk_import_internal_transfer_match_debit_transaction
        FOREIGN KEY (debit_transaction_id) REFERENCES money_transaction(id) ON DELETE SET NULL,
    CONSTRAINT fk_import_internal_transfer_match_credit_transaction
        FOREIGN KEY (credit_transaction_id) REFERENCES money_transaction(id) ON DELETE SET NULL,
    CONSTRAINT ck_import_internal_transfer_match_amount CHECK (amount > 0),
    CONSTRAINT ck_import_internal_transfer_match_confidence CHECK (confidence >= 0 AND confidence <= 1),
    CONSTRAINT ck_import_internal_transfer_match_distinct_rows CHECK (debit_import_row_id <> credit_import_row_id),
    CONSTRAINT uq_import_internal_transfer_match_rows UNIQUE (debit_import_row_id, credit_import_row_id)
);

CREATE INDEX idx_import_internal_transfer_match_profile_date
    ON import_internal_transfer_match(profile_id, operation_date);

CREATE INDEX idx_import_internal_transfer_match_debit_transaction
    ON import_internal_transfer_match(debit_transaction_id)
    WHERE debit_transaction_id IS NOT NULL;

CREATE INDEX idx_import_internal_transfer_match_credit_transaction
    ON import_internal_transfer_match(credit_transaction_id)
    WHERE credit_transaction_id IS NOT NULL;

WITH income_root AS (
    SELECT id
    FROM category
    WHERE profile_id IS NULL
      AND category_key = 'ingresos'
      AND active = TRUE
    LIMIT 1
)
INSERT INTO category (
    profile_id,
    parent_id,
    name,
    category_key,
    type,
    scope,
    default_movement_type,
    budgetable,
    technical,
    active
)
SELECT
    NULL,
    income_root.id,
    'Ingresos por servicios y proyectos',
    'ingresosporserviciosyproyectos',
    'INCOME',
    'GLOBAL',
    'INCOME',
    TRUE,
    FALSE,
    TRUE
FROM income_root
ON CONFLICT DO NOTHING;

UPDATE classification_rule
SET category_id = NULL,
    movement_type = 'INCOME',
    classification_status = 'REVIEW',
    warning = 'Transferencia/fondeo candidato: requiere match fuerte o revisión; no es interna por canal.',
    updated_at = now()
WHERE reason_code = 'RULE_MP_FUNDING_TRANSFER';
