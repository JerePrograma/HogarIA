CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE account
    ADD COLUMN IF NOT EXISTS account_key VARCHAR(120);

WITH normalized AS (
    SELECT
        id,
        NULLIF(
            lower(
                regexp_replace(
                    translate(
                        replace(name, chr(160), ' '),
                        'ÁÀÄÂÃáàäâãÉÈËÊéèëêÍÌÏÎíìïîÓÒÖÔÕóòöôõÚÙÜÛúùüûÑñÇç',
                        'AAAAAaaaaaEEEEeeeeIIIIiiiiOOOOOoooooUUUUuuuuNnCc'
                    ),
                    '[^a-zA-Z0-9]+',
                    '',
                    'g'
                )
            ),
            ''
        ) AS base_key
    FROM account
),
ranked AS (
    SELECT
        a.id,
        n.base_key,
        row_number() OVER (
            PARTITION BY a.profile_id, n.base_key, upper(a.currency)
            ORDER BY a.active DESC, a.created_at ASC, a.id ASC
        ) AS rn
    FROM account a
    JOIN normalized n ON n.id = a.id
    WHERE n.base_key IS NOT NULL
)
UPDATE account a
SET account_key = CASE
    WHEN ranked.rn = 1 THEN ranked.base_key
    ELSE ranked.base_key || '_' || replace(left(a.id::text, 8), '-', '')
END,
    currency = upper(a.currency)
FROM ranked
WHERE ranked.id = a.id
  AND (a.account_key IS NULL OR btrim(a.account_key) = '');

CREATE UNIQUE INDEX IF NOT EXISTS ux_account_profile_key_currency_active
    ON account(profile_id, account_key, currency)
    WHERE active = TRUE AND account_key IS NOT NULL;

DROP INDEX IF EXISTS ux_category_profile_key_type;
DROP INDEX IF EXISTS ux_category_global_key_type;

WITH normalized AS (
    SELECT
        id,
        NULLIF(
            lower(
                regexp_replace(
                    translate(
                        replace(name, chr(160), ' '),
                        'ÁÀÄÂÃáàäâãÉÈËÊéèëêÍÌÏÎíìïîÓÒÖÔÕóòöôõÚÙÜÛúùüûÑñÇç',
                        'AAAAAaaaaaEEEEeeeeIIIIiiiiOOOOOoooooUUUUuuuuNnCc'
                    ),
                    '[^a-zA-Z0-9]+',
                    '',
                    'g'
                )
            ),
            ''
        ) AS base_key
    FROM category
),
ranked AS (
    SELECT
        c.id,
        n.base_key,
        row_number() OVER (
            PARTITION BY c.profile_id, c.type, n.base_key
            ORDER BY c.active DESC, c.created_at ASC, c.id ASC
        ) AS rn
    FROM category c
    JOIN normalized n ON n.id = c.id
    WHERE n.base_key IS NOT NULL
)
UPDATE category c
SET category_key = CASE
    WHEN ranked.rn = 1 THEN ranked.base_key
    ELSE ranked.base_key || '_' || replace(left(c.id::text, 8), '-', '')
END
FROM ranked
WHERE ranked.id = c.id;

CREATE UNIQUE INDEX IF NOT EXISTS ux_category_profile_key_type
    ON category(profile_id, category_key, type)
    WHERE profile_id IS NOT NULL AND category_key IS NOT NULL AND active = TRUE;

CREATE UNIQUE INDEX IF NOT EXISTS ux_category_global_key_type
    ON category(category_key, type)
    WHERE profile_id IS NULL AND category_key IS NOT NULL AND active = TRUE;

ALTER TABLE money_transaction
    ALTER COLUMN movement_type TYPE VARCHAR(40),
    ALTER COLUMN origin TYPE VARCHAR(40),
    ALTER COLUMN status TYPE VARCHAR(40);

ALTER TABLE money_transaction
    ADD COLUMN IF NOT EXISTS normalized_description VARCHAR(500),
    ADD COLUMN IF NOT EXISTS operation_datetime TIMESTAMP,
    ADD COLUMN IF NOT EXISTS operation_datetime_precision VARCHAR(20) NOT NULL DEFAULT 'DATE_ONLY',
    ADD COLUMN IF NOT EXISTS duplicate_fingerprint VARCHAR(64),
    ADD COLUMN IF NOT EXISTS balance_impact VARCHAR(40) NOT NULL DEFAULT 'UNKNOWN';

ALTER TABLE money_transaction
DROP CONSTRAINT IF EXISTS chk_money_transaction_operation_datetime_precision;

ALTER TABLE money_transaction
    ADD CONSTRAINT chk_money_transaction_operation_datetime_precision
        CHECK (operation_datetime_precision IN ('DATE_ONLY', 'DATE_TIME'));

ALTER TABLE money_transaction
DROP CONSTRAINT IF EXISTS chk_money_transaction_balance_impact;

ALTER TABLE money_transaction
    ADD CONSTRAINT chk_money_transaction_balance_impact
        CHECK (balance_impact IN (
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
            'NEUTRAL_ADJUSTMENT',
            'IGNORED',
            'TECHNICAL',
            'UNKNOWN'
        ));

UPDATE money_transaction
SET normalized_description = NULLIF(
        upper(
            btrim(
                regexp_replace(
                    translate(
                        replace(coalesce(description, ''), chr(160), ' '),
                        'ÁÀÄÂÃáàäâãÉÈËÊéèëêÍÌÏÎíìïîÓÒÖÔÕóòöôõÚÙÜÛúùüûÑñÇç',
                        'AAAAAaaaaaEEEEeeeeIIIIiiiiOOOOOoooooUUUUuuuuNnCc'
                    ),
                    '[[:space:]]+',
                    ' ',
                    'g'
                )
            )
        ),
        ''
    ),
    operation_datetime = COALESCE(operation_datetime, real_date::timestamp),
    operation_datetime_precision = COALESCE(operation_datetime_precision, 'DATE_ONLY'),
    source = NULLIF(upper(btrim(source)), ''),
    source_operation_id = NULLIF(btrim(source_operation_id), ''),
    source_hash = NULLIF(btrim(source_hash), '')
WHERE normalized_description IS NULL
   OR operation_datetime IS NULL
   OR source <> upper(btrim(source))
   OR source_operation_id <> btrim(source_operation_id)
   OR source_hash <> btrim(source_hash);

UPDATE money_transaction
SET duplicate_fingerprint = encode(
    digest(
        profile_id::text || '|' ||
        account_id::text || '|' ||
        operation_datetime::text || '|' ||
        coalesce(normalized_description, '') || '|' ||
        to_char(round(amount, 2), 'FM999999999999999990.00') || '|' ||
        upper(currency) || '|' ||
        coalesce(source, '') || '|' ||
        coalesce(source_operation_id, ''),
        'sha256'
    ),
    'hex'
)
WHERE duplicate_fingerprint IS NULL;

UPDATE money_transaction
SET balance_impact = CASE
    WHEN status = 'IGNORED' OR classification_status = 'IGNORED_BY_RULE' THEN 'IGNORED'
    WHEN internal_transfer_group_id IS NOT NULL
        OR payment_channel = 'INTERNAL_TRANSFER'
        OR classification_reason IN ('POSSIBLE_INTERNAL_TRANSFER', 'INTERNAL_TRANSFER_MATCHED', 'USER_MARKED_INTERNAL_TRANSFER', 'TRANSFER_UNMATCHED')
        THEN 'INTERNAL_TRANSFER'
    WHEN classification_status = 'TECHNICAL' THEN 'TECHNICAL'
    WHEN classification_reason = 'CJPRESTAMOS_DISBURSEMENT' THEN 'RECOVERABLE_OUTFLOW'
    WHEN classification_reason = 'CJPRESTAMOS_PAYMENT_PRINCIPAL_RECOVERY' THEN 'PRINCIPAL_RECOVERY'
    WHEN classification_reason = 'CJPRESTAMOS_PAYMENT_INTEREST_INCOME' THEN 'INTEREST_INCOME'
    WHEN movement_type = 'INCOME' THEN 'OPERATING_INCOME'
    WHEN movement_type = 'EXPENSE' THEN 'CONSUMPTION_EXPENSE'
    WHEN movement_type = 'SAVING' THEN 'SAVING_OUTFLOW'
    WHEN movement_type = 'TRANSFER' THEN 'EXTERNAL_TRANSFER'
    WHEN movement_type = 'ADJUSTMENT' THEN 'NEUTRAL_ADJUSTMENT'
    ELSE 'UNKNOWN'
END;

WITH ranked AS (
    SELECT
        id,
        row_number() OVER (
            PARTITION BY profile_id, account_id, duplicate_fingerprint
            ORDER BY created_at ASC, id ASC
        ) AS rn
    FROM money_transaction
    WHERE duplicate_fingerprint IS NOT NULL
      AND status <> 'IGNORED'
      AND classification_status <> 'IGNORED_BY_RULE'
)
UPDATE money_transaction tx
SET status = 'IGNORED',
    classification_status = 'IGNORED_BY_RULE',
    classification_reason = 'MIGRATION_DUPLICATE_FINGERPRINT_BACKFILL',
    balance_impact = 'IGNORED',
    updated_at = now()
FROM ranked
WHERE ranked.id = tx.id
  AND ranked.rn > 1;

CREATE INDEX IF NOT EXISTS idx_money_tx_profile_operation_datetime
    ON money_transaction(profile_id, operation_datetime);

CREATE INDEX IF NOT EXISTS idx_money_tx_profile_duplicate_fingerprint
    ON money_transaction(profile_id, account_id, duplicate_fingerprint)
    WHERE duplicate_fingerprint IS NOT NULL;

WITH ranked AS (
    SELECT
        id,
        row_number() OVER (
            PARTITION BY profile_id, source, source_operation_id
            ORDER BY created_at ASC, id ASC
        ) AS rn
    FROM money_transaction
    WHERE source IS NOT NULL
      AND source_operation_id IS NOT NULL
      AND status <> 'IGNORED'
)
UPDATE money_transaction tx
SET status = 'IGNORED',
    classification_status = 'IGNORED_BY_RULE',
    classification_reason = 'MIGRATION_DUPLICATE_SOURCE_OPERATION_BACKFILL',
    balance_impact = 'IGNORED',
    updated_at = now()
FROM ranked
WHERE ranked.id = tx.id
  AND ranked.rn > 1;

CREATE UNIQUE INDEX IF NOT EXISTS ux_money_tx_profile_source_operation_idempotent
    ON money_transaction(profile_id, source, source_operation_id)
    WHERE source IS NOT NULL
      AND source_operation_id IS NOT NULL
      AND status <> 'IGNORED';

CREATE UNIQUE INDEX IF NOT EXISTS ux_money_tx_profile_account_duplicate_fingerprint_active
    ON money_transaction(profile_id, account_id, duplicate_fingerprint)
    WHERE duplicate_fingerprint IS NOT NULL
      AND status <> 'IGNORED'
      AND classification_status <> 'IGNORED_BY_RULE';
