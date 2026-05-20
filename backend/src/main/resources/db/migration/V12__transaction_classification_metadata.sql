ALTER TABLE category
    ADD COLUMN IF NOT EXISTS category_key VARCHAR(120),
    ADD COLUMN IF NOT EXISTS default_movement_type VARCHAR(40),
    ADD COLUMN IF NOT EXISTS budgetable BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS technical BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE category
DROP CONSTRAINT IF EXISTS chk_category_default_movement_type;

ALTER TABLE category
    ADD CONSTRAINT chk_category_default_movement_type
        CHECK (
            default_movement_type IS NULL
                OR default_movement_type IN ('INCOME', 'EXPENSE', 'SAVING', 'TRANSFER', 'ADJUSTMENT')
            );

UPDATE category
SET category_key = CASE
                       WHEN lower(name) = lower('Comisiones y cargos') THEN 'comisiones_cargos'
                       WHEN lower(name) = lower('Impuestos') THEN 'impuestos'
                       WHEN lower(name) = lower('Tarjeta de credito') THEN 'tarjeta_credito'
                       WHEN lower(name) = lower('Retiros de efectivo') THEN 'retiros_efectivo'
                       WHEN lower(name) = lower('Compras con tarjeta') THEN 'compras_tarjeta'
                       WHEN lower(name) = lower('Cuenta DNI / DEBIN') THEN 'cuenta_dni_debin'
                       WHEN lower(name) = lower('Sueldo / Ingresos laborales') THEN 'sueldo_ingresos_laborales'
                       WHEN lower(name) = lower('Transferencias recibidas') THEN 'transferencias_recibidas'
                       WHEN lower(name) = lower('Transferencias enviadas') THEN 'transferencias_enviadas'
                       WHEN lower(name) = lower('Devoluciones y reintegros') THEN 'devoluciones_reintegros'
                       WHEN lower(name) = lower('Beneficios y promociones') THEN 'beneficios_promociones'
                       WHEN lower(name) = lower('Comidas y bebidas') THEN 'comidas_bebidas'
                       WHEN lower(name) = lower('Supermercado') THEN 'supermercado'
                       WHEN lower(name) = lower('Transporte') THEN 'transporte'
                       WHEN lower(name) = lower('Suscripciones') THEN 'suscripciones'
                       WHEN lower(name) = lower('Salud y cuidado personal') THEN 'salud_cuidado_personal'
                       WHEN lower(name) = lower('Educacion') THEN 'educacion'
                       WHEN lower(name) = lower('Shopping') THEN 'shopping'
                       WHEN lower(name) = lower('Viajes') THEN 'viajes'
                       WHEN lower(name) = lower('Creditos y financiacion') THEN 'creditos_financiacion'
                       WHEN lower(name) = lower('Gastos generales') THEN 'gastos_generales'
                       WHEN lower(name) = lower('CJ - Capital recuperado') THEN 'cj_capital_recuperado'
                       WHEN lower(name) = lower('Fondeo MercadoPago / transferencias internas') THEN 'fondeo_mercadopago_transferencias_internas'
                       WHEN lower(name) = lower('Ajustes MercadoPago') THEN 'ajustes_mercadopago'
                       ELSE category_key
    END
WHERE category_key IS NULL;

UPDATE category
SET default_movement_type = CASE
                                WHEN type = 'INCOME' THEN 'INCOME'
                                WHEN type IN ('SAVING', 'INVESTMENT') THEN 'SAVING'
                                WHEN type = 'DEBT' THEN 'ADJUSTMENT'
                                ELSE 'EXPENSE'
    END
WHERE default_movement_type IS NULL;

UPDATE category
SET technical = TRUE,
    budgetable = FALSE
WHERE category_key IN (
                       'cuenta_dni_debin',
                       'fondeo_mercadopago_transferencias_internas',
                       'ajustes_mercadopago'
    );

CREATE UNIQUE INDEX IF NOT EXISTS ux_category_profile_key_type
    ON category(profile_id, category_key, type)
    WHERE profile_id IS NOT NULL AND category_key IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_category_global_key_type
    ON category(category_key, type)
    WHERE profile_id IS NULL AND category_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_category_profile_budgetable
    ON category(profile_id, budgetable, technical);

ALTER TABLE money_transaction
    ALTER COLUMN category_id DROP NOT NULL;

ALTER TABLE money_transaction
    ADD COLUMN IF NOT EXISTS source VARCHAR(40),
    ADD COLUMN IF NOT EXISTS source_operation_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS source_hash VARCHAR(64),
    ADD COLUMN IF NOT EXISTS payment_channel VARCHAR(40),
    ADD COLUMN IF NOT EXISTS counterparty VARCHAR(255),
    ADD COLUMN IF NOT EXISTS classification_status VARCHAR(40) NOT NULL DEFAULT 'CLASSIFIED',
    ADD COLUMN IF NOT EXISTS classification_reason VARCHAR(255),
    ADD COLUMN IF NOT EXISTS import_batch_id UUID,
    ADD COLUMN IF NOT EXISTS internal_transfer_group_id UUID;

ALTER TABLE money_transaction
DROP CONSTRAINT IF EXISTS chk_money_transaction_payment_channel;

ALTER TABLE money_transaction
    ADD CONSTRAINT chk_money_transaction_payment_channel
        CHECK (
            payment_channel IS NULL
                OR payment_channel IN (
                                       'UNKNOWN',
                                       'CASH',
                                       'BANK_TRANSFER',
                                       'DEBIN',
                                       'CUENTA_DNI',
                                       'DEBIT_CARD',
                                       'CREDIT_CARD',
                                       'MERCADO_PAGO',
                                       'MERCADO_CREDITO',
                                       'INTERNAL_TRANSFER',
                                       'OTHER'
                )
            );

ALTER TABLE money_transaction
DROP CONSTRAINT IF EXISTS chk_money_transaction_classification_status;

ALTER TABLE money_transaction
    ADD CONSTRAINT chk_money_transaction_classification_status
        CHECK (
            classification_status IN (
                                      'CLASSIFIED',
                                      'NEEDS_CATEGORY',
                                      'REVIEW',
                                      'TECHNICAL',
                                      'IGNORED_BY_RULE'
                )
            );

UPDATE money_transaction
SET classification_status = CASE
                                WHEN category_id IS NULL THEN 'NEEDS_CATEGORY'
                                ELSE 'CLASSIFIED'
    END
WHERE classification_status IS NULL
   OR classification_status = '';

CREATE INDEX IF NOT EXISTS idx_money_tx_profile_origin_real_date
    ON money_transaction(profile_id, origin, real_date);

CREATE INDEX IF NOT EXISTS idx_money_tx_profile_status_budget_date
    ON money_transaction(profile_id, status, budget_date);

CREATE INDEX IF NOT EXISTS idx_money_tx_profile_classification
    ON money_transaction(profile_id, classification_status, budget_date);

CREATE INDEX IF NOT EXISTS idx_money_tx_profile_source_operation
    ON money_transaction(profile_id, source, source_operation_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_money_tx_profile_source_hash
    ON money_transaction(profile_id, source_hash)
    WHERE source_hash IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_money_tx_internal_transfer_group
    ON money_transaction(internal_transfer_group_id)
    WHERE internal_transfer_group_id IS NOT NULL;