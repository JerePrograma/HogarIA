ALTER TABLE excel_import_batch
    ADD COLUMN IF NOT EXISTS account_id uuid;

ALTER TABLE excel_import_batch
    ADD COLUMN IF NOT EXISTS source varchar(40);

ALTER TABLE excel_import_batch
    ADD COLUMN IF NOT EXISTS month integer;

CREATE INDEX IF NOT EXISTS idx_excel_import_batch_profile_created
    ON excel_import_batch (profile_id, created_at);

CREATE INDEX IF NOT EXISTS idx_excel_import_batch_profile_source
    ON excel_import_batch (profile_id, source);

CREATE INDEX IF NOT EXISTS idx_excel_import_batch_account
    ON excel_import_batch (account_id);

ALTER TABLE excel_import_row
    ADD COLUMN IF NOT EXISTS real_date date;

ALTER TABLE excel_import_row
    ADD COLUMN IF NOT EXISTS budget_date date;

ALTER TABLE excel_import_row
    ADD COLUMN IF NOT EXISTS movement_type varchar(40);

ALTER TABLE excel_import_row
    ADD COLUMN IF NOT EXISTS source_operation_id varchar(120);

ALTER TABLE excel_import_row
    ADD COLUMN IF NOT EXISTS source_hash varchar(64);

ALTER TABLE excel_import_row
    ADD COLUMN IF NOT EXISTS external_sequence varchar(120);

ALTER TABLE excel_import_row
    ADD COLUMN IF NOT EXISTS raw_description varchar(255);

ALTER TABLE excel_import_row
    ADD COLUMN IF NOT EXISTS normalized_description varchar(500);

ALTER TABLE excel_import_row
    ADD COLUMN IF NOT EXISTS extended_description varchar(500);

ALTER TABLE excel_import_row
    ADD COLUMN IF NOT EXISTS merchant_name varchar(255);

CREATE INDEX IF NOT EXISTS idx_excel_import_row_batch_row
    ON excel_import_row (batch_id, row_number);

CREATE INDEX IF NOT EXISTS idx_excel_import_row_source_hash
    ON excel_import_row (source_hash);

CREATE INDEX IF NOT EXISTS idx_excel_import_row_source_operation
    ON excel_import_row (source_operation_id);

CREATE TABLE IF NOT EXISTS transaction_import_reference (
                                                            id uuid PRIMARY KEY DEFAULT gen_random_uuid(),

    transaction_id uuid NOT NULL,
    profile_id uuid NOT NULL,
    account_id uuid NOT NULL,

    import_batch_id uuid,
    import_row_id uuid,

    import_source varchar(40) NOT NULL,
    source_operation_id varchar(120),
    source_hash varchar(64),
    external_sequence varchar(120),

    raw_description varchar(255),
    normalized_description varchar(500),
    extended_description varchar(500),
    merchant_name varchar(255),
    raw_payload jsonb,

    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),

    CONSTRAINT fk_transaction_import_reference_transaction
    FOREIGN KEY (transaction_id)
    REFERENCES money_transaction(id)
    ON DELETE CASCADE,

    CONSTRAINT fk_transaction_import_reference_batch
    FOREIGN KEY (import_batch_id)
    REFERENCES excel_import_batch(id)
    ON DELETE SET NULL,

    CONSTRAINT fk_transaction_import_reference_row
    FOREIGN KEY (import_row_id)
    REFERENCES excel_import_row(id)
    ON DELETE SET NULL
    );

CREATE UNIQUE INDEX IF NOT EXISTS ux_transaction_import_reference_transaction
    ON transaction_import_reference (transaction_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_transaction_import_reference_import_row
    ON transaction_import_reference (import_row_id)
    WHERE import_row_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_transaction_import_reference_source_hash
    ON transaction_import_reference (profile_id, account_id, import_source, source_hash)
    WHERE source_hash IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_transaction_import_reference_transaction
    ON transaction_import_reference (transaction_id);

CREATE INDEX IF NOT EXISTS idx_transaction_import_reference_row
    ON transaction_import_reference (import_row_id);

CREATE INDEX IF NOT EXISTS idx_transaction_import_reference_batch
    ON transaction_import_reference (import_batch_id);

CREATE INDEX IF NOT EXISTS idx_transaction_import_reference_source_hash
    ON transaction_import_reference (source_hash);

CREATE INDEX IF NOT EXISTS idx_transaction_import_reference_source_operation
    ON transaction_import_reference (source_operation_id);

CREATE INDEX IF NOT EXISTS idx_transaction_import_reference_profile_account_source
    ON transaction_import_reference (profile_id, account_id, import_source);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'money_transaction'
          AND column_name = 'source_hash'
    ) THEN
        INSERT INTO transaction_import_reference (
            transaction_id,
            profile_id,
            account_id,
            import_batch_id,
            import_row_id,
            import_source,
            source_operation_id,
            source_hash,
            external_sequence,
            raw_description,
            extended_description,
            merchant_name,
            raw_payload,
            created_at,
            updated_at
        )
SELECT
    id,
    profile_id,
    account_id,
    import_batch_id,
    import_row_id,
    COALESCE(import_source, 'UNKNOWN'),
    source_operation_id,
    source_hash,
    external_sequence,
    raw_description,
    extended_description,
    merchant_name,
    raw_payload,
    now(),
    now()
FROM money_transaction
WHERE origin = 'IMPORT'
  AND (
    source_hash IS NOT NULL
        OR source_operation_id IS NOT NULL
        OR import_batch_id IS NOT NULL
        OR import_row_id IS NOT NULL
        OR raw_payload IS NOT NULL
    )
    ON CONFLICT DO NOTHING;
END IF;
END $$;

ALTER TABLE money_transaction DROP COLUMN IF EXISTS import_source;
ALTER TABLE money_transaction DROP COLUMN IF EXISTS source_operation_id;
ALTER TABLE money_transaction DROP COLUMN IF EXISTS source_hash;
ALTER TABLE money_transaction DROP COLUMN IF EXISTS import_batch_id;
ALTER TABLE money_transaction DROP COLUMN IF EXISTS import_row_id;
ALTER TABLE money_transaction DROP COLUMN IF EXISTS external_sequence;
ALTER TABLE money_transaction DROP COLUMN IF EXISTS raw_description;
ALTER TABLE money_transaction DROP COLUMN IF EXISTS extended_description;
ALTER TABLE money_transaction DROP COLUMN IF EXISTS merchant_name;
ALTER TABLE money_transaction DROP COLUMN IF EXISTS raw_payload;

CREATE INDEX IF NOT EXISTS idx_money_transaction_profile_budget_date
    ON money_transaction (profile_id, budget_date);

CREATE INDEX IF NOT EXISTS idx_money_transaction_profile_real_date
    ON money_transaction (profile_id, real_date);

CREATE INDEX IF NOT EXISTS idx_money_transaction_profile_account_real_amount
    ON money_transaction (profile_id, account_id, real_date, amount);