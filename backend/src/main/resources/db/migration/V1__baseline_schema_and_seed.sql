CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE app_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(120) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_app_user_email UNIQUE (email)
);

CREATE TABLE financial_profile (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL,
    base_currency VARCHAR(3) NOT NULL,
    active_year INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_financial_profile_user FOREIGN KEY (user_id) REFERENCES app_user(id),
    CONSTRAINT ck_financial_profile_type CHECK (type IN ('PERSONAL', 'FAMILY', 'BUSINESS')),
    CONSTRAINT ck_financial_profile_base_currency CHECK (char_length(base_currency) = 3),
    CONSTRAINT ck_financial_profile_active_year CHECK (active_year BETWEEN 2000 AND 2100),
    CONSTRAINT uk_financial_profile_user_name UNIQUE (user_id, name)
);

CREATE TABLE account (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    account_key VARCHAR(120),
    account_type VARCHAR(30) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    credit_limit NUMERIC(19,2),
    statement_close_day INT,
    due_day INT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_account_profile FOREIGN KEY (profile_id) REFERENCES financial_profile(id),
    CONSTRAINT ck_account_type CHECK (account_type IN ('CASH', 'BANK', 'CREDIT_CARD', 'DEBIT_CARD', 'VIRTUAL_WALLET', 'BUSINESS')),
    CONSTRAINT ck_account_currency CHECK (char_length(currency) = 3),
    CONSTRAINT ck_account_statement_close_day CHECK (statement_close_day IS NULL OR statement_close_day BETWEEN 1 AND 31),
    CONSTRAINT ck_account_due_day CHECK (due_day IS NULL OR due_day BETWEEN 1 AND 31),
    CONSTRAINT uk_account_profile_name UNIQUE (profile_id, name)
);

CREATE TABLE category (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID,
    parent_id UUID,
    name VARCHAR(120) NOT NULL,
    category_key VARCHAR(120),
    type VARCHAR(30) NOT NULL,
    scope VARCHAR(20) NOT NULL,
    default_movement_type VARCHAR(40),
    budgetable BOOLEAN NOT NULL DEFAULT TRUE,
    technical BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_category_profile FOREIGN KEY (profile_id) REFERENCES financial_profile(id),
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES category(id),
    CONSTRAINT ck_category_type CHECK (type IN ('INCOME', 'FIXED_EXPENSE', 'VARIABLE_EXPENSE', 'SAVING', 'DEBT', 'INVESTMENT')),
    CONSTRAINT ck_category_scope CHECK (scope IN ('PERSONAL', 'FAMILY', 'BUSINESS', 'GLOBAL')),
    CONSTRAINT ck_category_default_movement_type CHECK (
        default_movement_type IS NULL
        OR default_movement_type IN ('INCOME', 'EXPENSE', 'SAVING', 'TRANSFER', 'ADJUSTMENT')
    )
);

CREATE TABLE money_transaction (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL,
    account_id UUID NOT NULL,
    category_id UUID,
    movement_type VARCHAR(40) NOT NULL,
    real_date DATE NOT NULL,
    budget_date DATE NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    description VARCHAR(255),
    normalized_description VARCHAR(500),
    operation_datetime TIMESTAMP,
    operation_datetime_precision VARCHAR(20) NOT NULL DEFAULT 'DATE_ONLY',
    duplicate_fingerprint VARCHAR(64),
    balance_impact VARCHAR(40) NOT NULL DEFAULT 'UNKNOWN',
    origin VARCHAR(40) NOT NULL DEFAULT 'MANUAL',
    status VARCHAR(40) NOT NULL DEFAULT 'CONFIRMED',
    source VARCHAR(40),
    source_operation_id VARCHAR(120),
    source_hash VARCHAR(64),
    payment_channel VARCHAR(40),
    counterparty VARCHAR(255),
    classification_status VARCHAR(40) NOT NULL DEFAULT 'CLASSIFIED',
    classification_reason VARCHAR(255),
    classification_explanation_json JSONB,
    import_batch_id UUID,
    internal_transfer_group_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_money_transaction_profile FOREIGN KEY (profile_id) REFERENCES financial_profile(id),
    CONSTRAINT fk_money_transaction_account FOREIGN KEY (account_id) REFERENCES account(id),
    CONSTRAINT fk_money_transaction_category FOREIGN KEY (category_id) REFERENCES category(id),
    CONSTRAINT ck_money_transaction_movement_type CHECK (movement_type IN ('INCOME', 'EXPENSE', 'SAVING', 'TRANSFER', 'ADJUSTMENT')),
    CONSTRAINT ck_money_transaction_amount CHECK (amount > 0),
    CONSTRAINT ck_money_transaction_currency CHECK (char_length(currency) = 3),
    CONSTRAINT ck_money_transaction_origin CHECK (origin IN ('MANUAL', 'IMPORT', 'RECURRENT', 'SYSTEM')),
    CONSTRAINT ck_money_transaction_status CHECK (status IN ('CONFIRMED', 'PENDING', 'IGNORED')),
    CONSTRAINT ck_money_transaction_payment_channel CHECK (
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
            'DIRECT_DEBIT',
            'POS_TRANSFER',
            'ATM',
            'MONEY_MARKET_YIELD',
            'TRANSPORT_CARD',
            'QR_PAYMENT',
            'CARD_FOREIGN_CURRENCY',
            'OTHER'
        )
    ),
    CONSTRAINT ck_money_transaction_classification_status CHECK (
        classification_status IN ('CLASSIFIED', 'NEEDS_CATEGORY', 'REVIEW', 'TECHNICAL', 'IGNORED_BY_RULE')
    ),
    CONSTRAINT ck_money_transaction_operation_datetime_precision CHECK (operation_datetime_precision IN ('DATE_ONLY', 'DATE_TIME')),
    CONSTRAINT ck_money_transaction_balance_impact CHECK (
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
            'NEUTRAL_ADJUSTMENT',
            'IGNORED',
            'TECHNICAL',
            'UNKNOWN'
        )
    )
);

CREATE TABLE budget_year (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL,
    year INT NOT NULL,
    target_income NUMERIC(19,2),
    target_saving NUMERIC(19,2),
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_budget_year_profile FOREIGN KEY (profile_id) REFERENCES financial_profile(id),
    CONSTRAINT ck_budget_year_year CHECK (year BETWEEN 2000 AND 2100),
    CONSTRAINT uk_budget_year_profile_year UNIQUE (profile_id, year)
);

CREATE TABLE budget_month (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    budget_year_id UUID NOT NULL,
    month INT NOT NULL,
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_budget_month_year FOREIGN KEY (budget_year_id) REFERENCES budget_year(id),
    CONSTRAINT ck_budget_month_month CHECK (month BETWEEN 1 AND 12),
    CONSTRAINT uk_budget_month_year_month UNIQUE (budget_year_id, month)
);

CREATE TABLE budget_category_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    budget_month_id UUID NOT NULL,
    category_id UUID NOT NULL,
    budget_amount NUMERIC(19,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_budget_category_item_month FOREIGN KEY (budget_month_id) REFERENCES budget_month(id),
    CONSTRAINT fk_budget_category_item_category FOREIGN KEY (category_id) REFERENCES category(id),
    CONSTRAINT ck_budget_category_item_amount CHECK (budget_amount >= 0),
    CONSTRAINT uk_budget_category_item_month_category UNIQUE (budget_month_id, category_id)
);

CREATE TABLE financial_goal (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL,
    name VARCHAR(160) NOT NULL,
    goal_type VARCHAR(40) NOT NULL,
    target_amount NUMERIC(19,2) NOT NULL,
    current_amount NUMERIC(19,2) NOT NULL DEFAULT 0,
    monthly_contribution NUMERIC(19,2),
    target_date DATE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_financial_goal_profile FOREIGN KEY (profile_id) REFERENCES financial_profile(id),
    CONSTRAINT ck_financial_goal_type CHECK (
        goal_type IN ('EMERGENCY_FUND', 'DEBT_PAYOFF', 'SAVING_TARGET', 'INVESTMENT', 'BUSINESS', 'TRAVEL', 'EDUCATION', 'OTHER')
    ),
    CONSTRAINT ck_financial_goal_target_amount CHECK (target_amount >= 0),
    CONSTRAINT ck_financial_goal_current_amount CHECK (current_amount >= 0),
    CONSTRAINT ck_financial_goal_monthly_contribution CHECK (monthly_contribution IS NULL OR monthly_contribution >= 0),
    CONSTRAINT ck_financial_goal_status CHECK (status IN ('ACTIVE', 'PAUSED', 'COMPLETED', 'CANCELLED'))
);

CREATE TABLE habit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL,
    description VARCHAR(255) NOT NULL,
    area VARCHAR(80),
    frequency VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_habit_profile FOREIGN KEY (profile_id) REFERENCES financial_profile(id),
    CONSTRAINT ck_habit_frequency CHECK (frequency IN ('DAILY', 'WEEKLY', 'MONTHLY'))
);

CREATE TABLE habit_checkin (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    habit_id UUID NOT NULL,
    checkin_date DATE NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    note VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_habit_checkin_habit FOREIGN KEY (habit_id) REFERENCES habit(id) ON DELETE CASCADE,
    CONSTRAINT uk_habit_checkin_habit_date UNIQUE (habit_id, checkin_date)
);

CREATE TABLE inflation_index (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    year INT NOT NULL,
    month INT NOT NULL,
    category_code VARCHAR(80),
    category_name VARCHAR(160),
    monthly_rate NUMERIC(12,6) NOT NULL,
    source VARCHAR(120),
    projection BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT ck_inflation_index_year CHECK (year BETWEEN 2000 AND 2100),
    CONSTRAINT ck_inflation_index_month CHECK (month BETWEEN 1 AND 12)
);

CREATE TABLE excel_import_batch (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL,
    account_id UUID,
    source VARCHAR(40),
    original_file_name VARCHAR(255),
    status VARCHAR(30) NOT NULL,
    detected_profile_type VARCHAR(30),
    year INT,
    month INT,
    currency VARCHAR(3) NOT NULL DEFAULT 'ARS',
    summary_json JSONB,
    warnings_json JSONB,
    errors_json JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_excel_import_batch_profile FOREIGN KEY (profile_id) REFERENCES financial_profile(id),
    CONSTRAINT ck_excel_import_batch_status CHECK (status IN ('PREVIEWED', 'PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT ck_excel_import_batch_profile_type CHECK (
        detected_profile_type IS NULL OR detected_profile_type IN ('PERSONAL', 'FAMILY', 'BUSINESS')
    ),
    CONSTRAINT ck_excel_import_batch_year CHECK (year IS NULL OR year BETWEEN 2000 AND 2100),
    CONSTRAINT ck_excel_import_batch_month CHECK (month IS NULL OR month BETWEEN 1 AND 12),
    CONSTRAINT ck_excel_import_batch_currency CHECK (char_length(currency) = 3)
);

CREATE TABLE excel_import_row (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id UUID NOT NULL,
    sheet_name VARCHAR(120) NOT NULL,
    row_number INT,
    concept VARCHAR(255),
    month INT,
    real_date DATE,
    budget_date DATE,
    amount NUMERIC(19,2),
    movement_type VARCHAR(40),
    source_operation_id VARCHAR(120),
    source_hash VARCHAR(64),
    external_sequence VARCHAR(120),
    raw_description VARCHAR(255),
    normalized_description VARCHAR(500),
    extended_description VARCHAR(500),
    merchant_name VARCHAR(255),
    counterparty_name VARCHAR(255),
    counterparty_document_hash VARCHAR(64),
    payment_channel VARCHAR(40),
    classification_status VARCHAR(40),
    classification_reason VARCHAR(255),
    classification_explanation_json JSONB,
    target_entity VARCHAR(40),
    status VARCHAR(30) NOT NULL,
    error_message VARCHAR(1000),
    raw_json JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_excel_import_row_batch FOREIGN KEY (batch_id) REFERENCES excel_import_batch(id) ON DELETE CASCADE,
    CONSTRAINT ck_excel_import_row_month CHECK (month IS NULL OR month BETWEEN 1 AND 12),
    CONSTRAINT ck_excel_import_row_movement_type CHECK (
        movement_type IS NULL OR movement_type IN ('INCOME', 'EXPENSE', 'SAVING', 'TRANSFER', 'ADJUSTMENT')
    ),
    CONSTRAINT ck_excel_import_row_payment_channel CHECK (
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
            'DIRECT_DEBIT',
            'POS_TRANSFER',
            'ATM',
            'MONEY_MARKET_YIELD',
            'TRANSPORT_CARD',
            'QR_PAYMENT',
            'CARD_FOREIGN_CURRENCY',
            'OTHER'
        )
    ),
    CONSTRAINT ck_excel_import_row_classification_status CHECK (
        classification_status IS NULL
        OR classification_status IN ('CLASSIFIED', 'NEEDS_CATEGORY', 'REVIEW', 'TECHNICAL', 'IGNORED_BY_RULE')
    ),
    CONSTRAINT ck_excel_import_row_target_entity CHECK (
        target_entity IS NULL
        OR target_entity IN (
            'CATEGORY',
            'ACCOUNT',
            'INCOME',
            'EXPENSE',
            'SAVING',
            'BUDGET',
            'CARD',
            'GOAL',
            'HABIT',
            'HABIT_CHECKIN',
            'INFLATION',
            'UNKNOWN'
        )
    ),
    CONSTRAINT ck_excel_import_row_status CHECK (status IN ('READY', 'IMPORTED', 'SKIPPED', 'WARNING', 'ERROR'))
);

CREATE TABLE merchant_alias (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID,
    source VARCHAR(40),
    alias_raw VARCHAR(255) NOT NULL,
    alias_normalized VARCHAR(255) NOT NULL,
    canonical_name VARCHAR(255) NOT NULL,
    category_id UUID NOT NULL,
    payment_channel VARCHAR(40),
    confidence NUMERIC(5,2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_merchant_alias_profile FOREIGN KEY (profile_id) REFERENCES financial_profile(id),
    CONSTRAINT fk_merchant_alias_category FOREIGN KEY (category_id) REFERENCES category(id),
    CONSTRAINT ck_merchant_alias_confidence CHECK (confidence >= 0 AND confidence <= 1),
    CONSTRAINT ck_merchant_alias_payment_channel CHECK (
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
            'DIRECT_DEBIT',
            'POS_TRANSFER',
            'ATM',
            'MONEY_MARKET_YIELD',
            'TRANSPORT_CARD',
            'QR_PAYMENT',
            'CARD_FOREIGN_CURRENCY',
            'OTHER'
        )
    )
);

CREATE TABLE counterparty_alias (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL,
    source VARCHAR(40),
    counterparty_name VARCHAR(255),
    counterparty_document_hash VARCHAR(64),
    canonical_name VARCHAR(255) NOT NULL,
    default_category_id UUID,
    default_movement_type VARCHAR(40),
    internal_transfer_candidate BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_counterparty_alias_profile FOREIGN KEY (profile_id) REFERENCES financial_profile(id),
    CONSTRAINT fk_counterparty_alias_category FOREIGN KEY (default_category_id) REFERENCES category(id),
    CONSTRAINT ck_counterparty_alias_default_movement_type CHECK (
        default_movement_type IS NULL
        OR default_movement_type IN ('INCOME', 'EXPENSE', 'SAVING', 'TRANSFER', 'ADJUSTMENT')
    ),
    CONSTRAINT ck_counterparty_alias_identity CHECK (
        counterparty_name IS NOT NULL OR counterparty_document_hash IS NOT NULL
    )
);

CREATE TABLE classification_rule (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID,
    priority INT NOT NULL,
    source VARCHAR(40),
    field_name VARCHAR(80) NOT NULL,
    pattern VARCHAR(500) NOT NULL,
    pattern_type VARCHAR(20) NOT NULL,
    category_id UUID,
    movement_type VARCHAR(40),
    payment_channel VARCHAR(40),
    classification_status VARCHAR(40) NOT NULL,
    confidence NUMERIC(5,2) NOT NULL,
    reason_code VARCHAR(120) NOT NULL,
    warning VARCHAR(1000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_classification_rule_profile FOREIGN KEY (profile_id) REFERENCES financial_profile(id),
    CONSTRAINT fk_classification_rule_category FOREIGN KEY (category_id) REFERENCES category(id),
    CONSTRAINT ck_classification_rule_priority CHECK (priority >= 0),
    CONSTRAINT ck_classification_rule_pattern_type CHECK (pattern_type IN ('EXACT', 'CONTAINS', 'REGEX')),
    CONSTRAINT ck_classification_rule_movement_type CHECK (
        movement_type IS NULL OR movement_type IN ('INCOME', 'EXPENSE', 'SAVING', 'TRANSFER', 'ADJUSTMENT')
    ),
    CONSTRAINT ck_classification_rule_payment_channel CHECK (
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
            'DIRECT_DEBIT',
            'POS_TRANSFER',
            'ATM',
            'MONEY_MARKET_YIELD',
            'TRANSPORT_CARD',
            'QR_PAYMENT',
            'CARD_FOREIGN_CURRENCY',
            'OTHER'
        )
    ),
    CONSTRAINT ck_classification_rule_status CHECK (
        classification_status IN ('CLASSIFIED', 'NEEDS_CATEGORY', 'REVIEW', 'TECHNICAL', 'IGNORED_BY_RULE')
    ),
    CONSTRAINT ck_classification_rule_confidence CHECK (confidence >= 0 AND confidence <= 1)
);

CREATE TABLE transaction_classification_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID,
    import_row_id UUID,
    rule_id UUID,
    reason_code VARCHAR(120) NOT NULL,
    matched_field VARCHAR(80),
    matched_value VARCHAR(500),
    suggested_category_id UUID,
    confidence NUMERIC(5,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_tx_classification_audit_transaction FOREIGN KEY (transaction_id) REFERENCES money_transaction(id) ON DELETE CASCADE,
    CONSTRAINT fk_tx_classification_audit_import_row FOREIGN KEY (import_row_id) REFERENCES excel_import_row(id) ON DELETE SET NULL,
    CONSTRAINT fk_tx_classification_audit_rule FOREIGN KEY (rule_id) REFERENCES classification_rule(id) ON DELETE SET NULL,
    CONSTRAINT fk_tx_classification_audit_category FOREIGN KEY (suggested_category_id) REFERENCES category(id),
    CONSTRAINT ck_tx_classification_audit_confidence CHECK (confidence >= 0 AND confidence <= 1)
);

CREATE TABLE monthly_plan_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL,
    category_id UUID,
    account_id UUID,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(500),
    expected_date DATE,
    period_year INT NOT NULL,
    period_month INT NOT NULL,
    amount NUMERIC(19,2),
    min_amount NUMERIC(19,2),
    max_amount NUMERIC(19,2),
    currency VARCHAR(3) NOT NULL DEFAULT 'ARS',
    expected_recovery_amount NUMERIC(19,2),
    expected_recovery_percent NUMERIC(5,2),
    counterparty VARCHAR(120),
    installment_number INT,
    installment_total INT,
    priority VARCHAR(20) NOT NULL DEFAULT 'IMPORTANT',
    status VARCHAR(30) NOT NULL DEFAULT 'ESTIMATED',
    source VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    transaction_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_monthly_plan_item_profile FOREIGN KEY (profile_id) REFERENCES financial_profile(id),
    CONSTRAINT fk_monthly_plan_item_category FOREIGN KEY (category_id) REFERENCES category(id),
    CONSTRAINT fk_monthly_plan_item_account FOREIGN KEY (account_id) REFERENCES account(id),
    CONSTRAINT fk_monthly_plan_item_transaction FOREIGN KEY (transaction_id) REFERENCES money_transaction(id),
    CONSTRAINT ck_monthly_plan_item_type CHECK (type IN ('INCOME', 'EXPENSE', 'SAVING', 'DEBT', 'TRANSFER', 'RECOVERY', 'TODO')),
    CONSTRAINT ck_monthly_plan_item_period_year CHECK (period_year BETWEEN 2000 AND 2100),
    CONSTRAINT ck_monthly_plan_item_period_month CHECK (period_month BETWEEN 1 AND 12),
    CONSTRAINT ck_monthly_plan_item_amount CHECK (amount IS NULL OR amount >= 0),
    CONSTRAINT ck_monthly_plan_item_min_amount CHECK (min_amount IS NULL OR min_amount >= 0),
    CONSTRAINT ck_monthly_plan_item_max_amount CHECK (max_amount IS NULL OR max_amount >= 0),
    CONSTRAINT ck_monthly_plan_item_currency CHECK (char_length(currency) = 3),
    CONSTRAINT ck_monthly_plan_item_expected_recovery_amount CHECK (expected_recovery_amount IS NULL OR expected_recovery_amount >= 0),
    CONSTRAINT ck_monthly_plan_item_expected_recovery_percent CHECK (
        expected_recovery_percent IS NULL OR (expected_recovery_percent >= 0 AND expected_recovery_percent <= 100)
    ),
    CONSTRAINT ck_monthly_plan_item_installment_number CHECK (installment_number IS NULL OR installment_number > 0),
    CONSTRAINT ck_monthly_plan_item_installment_total CHECK (installment_total IS NULL OR installment_total > 0),
    CONSTRAINT ck_monthly_plan_item_installment_range CHECK (
        installment_number IS NULL OR installment_total IS NULL OR installment_number <= installment_total
    ),
    CONSTRAINT ck_monthly_plan_item_priority CHECK (priority IN ('ESSENTIAL', 'IMPORTANT', 'OPTIONAL')),
    CONSTRAINT ck_monthly_plan_item_status CHECK (status IN ('DRAFT', 'ESTIMATED', 'SCHEDULED', 'DUE', 'PAID', 'COLLECTED', 'CANCELLED')),
    CONSTRAINT ck_monthly_plan_item_source CHECK (source IN ('MANUAL', 'IMPORT', 'QUICK_CAPTURE', 'SYSTEM'))
);

CREATE TABLE monthly_plan_transaction_match (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL,
    monthly_plan_item_id UUID NOT NULL,
    money_transaction_id UUID NOT NULL,
    matched_amount NUMERIC(19,2) NOT NULL,
    match_type VARCHAR(40) NOT NULL,
    confidence VARCHAR(20) NOT NULL,
    note VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_monthly_plan_tx_match_item FOREIGN KEY (monthly_plan_item_id) REFERENCES monthly_plan_item(id),
    CONSTRAINT fk_monthly_plan_tx_match_tx FOREIGN KEY (money_transaction_id) REFERENCES money_transaction(id),
    CONSTRAINT ck_monthly_plan_tx_match_amount CHECK (matched_amount >= 0),
    CONSTRAINT ck_monthly_plan_tx_match_type CHECK (match_type IN ('MANUAL', 'SYSTEM_CONVERSION', 'SUGGESTED')),
    CONSTRAINT ck_monthly_plan_tx_match_confidence CHECK (confidence IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT uq_monthly_plan_tx_match_pair UNIQUE (profile_id, monthly_plan_item_id, money_transaction_id)
);

CREATE TABLE external_loan_sync_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL,
    account_id UUID,
    loan_disbursement_category_id UUID,
    principal_recovery_category_id UUID,
    interest_income_category_id UUID,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_external_loan_sync_config_profile FOREIGN KEY (profile_id) REFERENCES financial_profile(id),
    CONSTRAINT fk_external_loan_sync_config_account FOREIGN KEY (account_id) REFERENCES account(id),
    CONSTRAINT fk_external_loan_sync_config_loan_disbursement_category FOREIGN KEY (loan_disbursement_category_id) REFERENCES category(id),
    CONSTRAINT fk_external_loan_sync_config_principal_recovery_category FOREIGN KEY (principal_recovery_category_id) REFERENCES category(id),
    CONSTRAINT fk_external_loan_sync_config_interest_income_category FOREIGN KEY (interest_income_category_id) REFERENCES category(id),
    CONSTRAINT uk_external_loan_sync_config_profile UNIQUE (profile_id)
);

CREATE TABLE external_sync_mapping (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL,
    external_system VARCHAR(40) NOT NULL,
    external_entity_type VARCHAR(40) NOT NULL,
    external_entity_id VARCHAR(80) NOT NULL,
    external_event_type VARCHAR(40) NOT NULL,
    money_transaction_id UUID,
    monthly_plan_item_id UUID,
    event_hash VARCHAR(128),
    status VARCHAR(30) NOT NULL,
    error_message VARCHAR(1000),
    synced_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_external_sync_mapping_profile FOREIGN KEY (profile_id) REFERENCES financial_profile(id),
    CONSTRAINT fk_external_sync_mapping_money_transaction FOREIGN KEY (money_transaction_id) REFERENCES money_transaction(id),
    CONSTRAINT fk_external_sync_mapping_monthly_plan_item FOREIGN KEY (monthly_plan_item_id) REFERENCES monthly_plan_item(id),
    CONSTRAINT uk_external_sync_mapping_external_event_profile UNIQUE (
        profile_id,
        external_system,
        external_entity_type,
        external_entity_id,
        external_event_type
    )
);

CREATE TABLE transaction_import_reference (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL,
    profile_id UUID NOT NULL,
    account_id UUID NOT NULL,
    import_batch_id UUID,
    import_row_id UUID,
    import_source VARCHAR(40) NOT NULL,
    source_operation_id VARCHAR(120),
    source_hash VARCHAR(64),
    external_sequence VARCHAR(120),
    raw_description VARCHAR(255),
    normalized_description VARCHAR(500),
    extended_description VARCHAR(500),
    merchant_name VARCHAR(255),
    counterparty_name VARCHAR(255),
    counterparty_document_hash VARCHAR(64),
    payment_channel VARCHAR(40),
    classification_status VARCHAR(40),
    classification_reason VARCHAR(255),
    classification_explanation_json JSONB,
    raw_payload JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_transaction_import_reference_transaction FOREIGN KEY (transaction_id) REFERENCES money_transaction(id) ON DELETE CASCADE,
    CONSTRAINT fk_transaction_import_reference_batch FOREIGN KEY (import_batch_id) REFERENCES excel_import_batch(id) ON DELETE SET NULL,
    CONSTRAINT fk_transaction_import_reference_row FOREIGN KEY (import_row_id) REFERENCES excel_import_row(id) ON DELETE SET NULL,
    CONSTRAINT ck_transaction_import_reference_payment_channel CHECK (
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
            'DIRECT_DEBIT',
            'POS_TRANSFER',
            'ATM',
            'MONEY_MARKET_YIELD',
            'TRANSPORT_CARD',
            'QR_PAYMENT',
            'CARD_FOREIGN_CURRENCY',
            'OTHER'
        )
    ),
    CONSTRAINT ck_transaction_import_reference_classification_status CHECK (
        classification_status IS NULL
        OR classification_status IN ('CLASSIFIED', 'NEEDS_CATEGORY', 'REVIEW', 'TECHNICAL', 'IGNORED_BY_RULE')
    )
);

CREATE INDEX idx_profile_user_id ON financial_profile(user_id);

CREATE INDEX idx_account_profile_id ON account(profile_id);
CREATE UNIQUE INDEX ux_account_profile_key_currency_active
    ON account(profile_id, account_key, currency)
    WHERE active = TRUE AND account_key IS NOT NULL;

CREATE INDEX idx_category_profile_id ON category(profile_id);
CREATE UNIQUE INDEX ux_category_profile_name_type
    ON category(profile_id, name, type)
    WHERE profile_id IS NOT NULL;
CREATE UNIQUE INDEX ux_category_global_name_type
    ON category(name, type)
    WHERE profile_id IS NULL;
CREATE UNIQUE INDEX ux_category_profile_key_type
    ON category(profile_id, category_key, type)
    WHERE profile_id IS NOT NULL AND category_key IS NOT NULL AND active = TRUE;
CREATE UNIQUE INDEX ux_category_global_key_type
    ON category(category_key, type)
    WHERE profile_id IS NULL AND category_key IS NOT NULL AND active = TRUE;
CREATE INDEX idx_category_profile_budgetable_technical
    ON category(profile_id, budgetable, technical);
CREATE INDEX idx_category_global_category_key
    ON category(category_key)
    WHERE profile_id IS NULL;

CREATE INDEX idx_money_tx_profile_category ON money_transaction(profile_id, category_id);
CREATE INDEX idx_money_tx_profile_account ON money_transaction(profile_id, account_id);
CREATE INDEX idx_money_tx_type ON money_transaction(movement_type);
CREATE INDEX idx_money_tx_status ON money_transaction(status);
CREATE INDEX idx_money_transaction_profile_budget_date ON money_transaction(profile_id, budget_date);
CREATE INDEX idx_money_transaction_profile_real_date ON money_transaction(profile_id, real_date);
CREATE INDEX idx_money_transaction_profile_account_real_amount ON money_transaction(profile_id, account_id, real_date, amount);
CREATE INDEX idx_money_tx_profile_origin_real_date ON money_transaction(profile_id, origin, real_date);
CREATE INDEX idx_money_tx_profile_status_budget_date ON money_transaction(profile_id, status, budget_date);
CREATE INDEX idx_money_tx_profile_classification ON money_transaction(profile_id, classification_status, budget_date);
CREATE INDEX idx_money_tx_profile_source_operation ON money_transaction(profile_id, source, source_operation_id);
CREATE UNIQUE INDEX ux_money_tx_profile_source_hash
    ON money_transaction(profile_id, source_hash)
    WHERE source_hash IS NOT NULL
      AND status <> 'IGNORED'
      AND classification_status <> 'IGNORED_BY_RULE';
CREATE INDEX idx_money_tx_internal_transfer_group
    ON money_transaction(internal_transfer_group_id)
    WHERE internal_transfer_group_id IS NOT NULL;
CREATE INDEX idx_money_tx_profile_operation_datetime ON money_transaction(profile_id, operation_datetime);
CREATE INDEX idx_money_tx_profile_duplicate_fingerprint
    ON money_transaction(profile_id, account_id, duplicate_fingerprint)
    WHERE duplicate_fingerprint IS NOT NULL;
CREATE UNIQUE INDEX ux_money_tx_profile_source_operation_idempotent
    ON money_transaction(profile_id, source, source_operation_id)
    WHERE source IS NOT NULL
      AND source_operation_id IS NOT NULL
      AND status <> 'IGNORED'
      AND classification_status <> 'IGNORED_BY_RULE';
CREATE UNIQUE INDEX ux_money_tx_profile_account_duplicate_fingerprint_active
    ON money_transaction(profile_id, account_id, duplicate_fingerprint)
    WHERE duplicate_fingerprint IS NOT NULL
      AND status <> 'IGNORED'
      AND classification_status <> 'IGNORED_BY_RULE';
CREATE INDEX idx_budget_year_profile_year ON budget_year(profile_id, year);
CREATE INDEX idx_budget_month_year_month ON budget_month(budget_year_id, month);
CREATE INDEX idx_budget_item_month ON budget_category_item(budget_month_id);
CREATE INDEX idx_budget_item_category ON budget_category_item(category_id);

CREATE INDEX idx_financial_goal_profile ON financial_goal(profile_id);
CREATE INDEX idx_financial_goal_status ON financial_goal(status);
CREATE INDEX idx_financial_goal_type ON financial_goal(goal_type);

CREATE INDEX idx_habit_profile ON habit(profile_id);
CREATE INDEX idx_habit_active ON habit(active);
CREATE UNIQUE INDEX ux_habit_profile_description_frequency
    ON habit(profile_id, lower(trim(description)), frequency);

CREATE INDEX idx_habit_checkin_habit ON habit_checkin(habit_id);
CREATE INDEX idx_habit_checkin_date ON habit_checkin(checkin_date);
CREATE INDEX idx_habit_checkin_completed ON habit_checkin(completed);

CREATE UNIQUE INDEX ux_inflation_year_month_category
    ON inflation_index(year, month, COALESCE(category_code, 'GENERAL'));
CREATE INDEX idx_inflation_year ON inflation_index(year);
CREATE INDEX idx_inflation_month ON inflation_index(month);
CREATE INDEX idx_inflation_projection ON inflation_index(projection);
CREATE INDEX idx_inflation_source ON inflation_index(source);

CREATE INDEX idx_excel_import_batch_profile ON excel_import_batch(profile_id);
CREATE INDEX idx_excel_import_batch_status ON excel_import_batch(status);
CREATE INDEX idx_excel_import_batch_year ON excel_import_batch(year);
CREATE INDEX idx_excel_import_batch_profile_created ON excel_import_batch(profile_id, created_at);
CREATE INDEX idx_excel_import_batch_profile_source ON excel_import_batch(profile_id, source);
CREATE INDEX idx_excel_import_batch_account ON excel_import_batch(account_id);

CREATE INDEX idx_excel_import_row_batch ON excel_import_row(batch_id);
CREATE INDEX idx_excel_import_row_status ON excel_import_row(status);
CREATE INDEX idx_excel_import_row_target_entity ON excel_import_row(target_entity);
CREATE INDEX idx_excel_import_row_sheet ON excel_import_row(sheet_name);
CREATE INDEX idx_excel_import_row_batch_row ON excel_import_row(batch_id, row_number);
CREATE INDEX idx_excel_import_row_source_hash ON excel_import_row(source_hash);
CREATE INDEX idx_excel_import_row_source_operation ON excel_import_row(source_operation_id);
CREATE INDEX idx_excel_import_row_classification ON excel_import_row(classification_status);
CREATE INDEX idx_excel_import_row_payment_channel ON excel_import_row(payment_channel);

CREATE UNIQUE INDEX ux_merchant_alias_global_source_alias
    ON merchant_alias(source, alias_normalized)
    WHERE profile_id IS NULL AND active = TRUE;
CREATE UNIQUE INDEX ux_merchant_alias_profile_source_alias
    ON merchant_alias(profile_id, source, alias_normalized)
    WHERE profile_id IS NOT NULL AND active = TRUE;
CREATE INDEX idx_merchant_alias_profile_source ON merchant_alias(profile_id, source);
CREATE INDEX idx_merchant_alias_normalized ON merchant_alias(alias_normalized);
CREATE INDEX idx_merchant_alias_category ON merchant_alias(category_id);

CREATE UNIQUE INDEX ux_counterparty_alias_profile_source_document
    ON counterparty_alias(profile_id, source, counterparty_document_hash)
    WHERE counterparty_document_hash IS NOT NULL AND active = TRUE;
CREATE INDEX idx_counterparty_alias_profile_source ON counterparty_alias(profile_id, source);
CREATE INDEX idx_counterparty_alias_document_hash ON counterparty_alias(counterparty_document_hash);
CREATE INDEX idx_counterparty_alias_category ON counterparty_alias(default_category_id);

CREATE INDEX idx_classification_rule_profile_source ON classification_rule(profile_id, source);
CREATE INDEX idx_classification_rule_priority ON classification_rule(priority);
CREATE INDEX idx_classification_rule_category ON classification_rule(category_id);
CREATE UNIQUE INDEX ux_classification_rule_profile_reason
    ON classification_rule(COALESCE(profile_id, '00000000-0000-0000-0000-000000000000'::uuid), reason_code)
    WHERE active = TRUE;

CREATE INDEX idx_tx_classification_audit_transaction ON transaction_classification_audit(transaction_id);
CREATE INDEX idx_tx_classification_audit_import_row ON transaction_classification_audit(import_row_id);
CREATE INDEX idx_tx_classification_audit_rule ON transaction_classification_audit(rule_id);

CREATE INDEX idx_monthly_plan_item_profile_period ON monthly_plan_item(profile_id, period_year, period_month);
CREATE INDEX idx_monthly_plan_item_profile_transaction
    ON monthly_plan_item(profile_id, transaction_id)
    WHERE transaction_id IS NOT NULL;

CREATE INDEX idx_monthly_plan_tx_match_profile ON monthly_plan_transaction_match(profile_id);
CREATE INDEX idx_monthly_plan_tx_match_item ON monthly_plan_transaction_match(monthly_plan_item_id);
CREATE INDEX idx_monthly_plan_tx_match_tx ON monthly_plan_transaction_match(money_transaction_id);
CREATE INDEX idx_monthly_plan_tx_match_profile_transaction
    ON monthly_plan_transaction_match(profile_id, money_transaction_id);

CREATE INDEX idx_external_sync_mapping_profile ON external_sync_mapping(profile_id);
CREATE INDEX idx_external_sync_mapping_status ON external_sync_mapping(status);
CREATE INDEX idx_external_sync_mapping_profile_transaction
    ON external_sync_mapping(profile_id, money_transaction_id)
    WHERE money_transaction_id IS NOT NULL;

CREATE UNIQUE INDEX ux_transaction_import_reference_transaction
    ON transaction_import_reference(transaction_id);
CREATE UNIQUE INDEX ux_transaction_import_reference_import_row
    ON transaction_import_reference(import_row_id)
    WHERE import_row_id IS NOT NULL;
CREATE INDEX idx_transaction_import_reference_profile_source_hash
    ON transaction_import_reference(profile_id, account_id, import_source, source_hash)
    WHERE source_hash IS NOT NULL;
CREATE INDEX idx_transaction_import_reference_transaction ON transaction_import_reference(transaction_id);
CREATE INDEX idx_transaction_import_reference_row ON transaction_import_reference(import_row_id);
CREATE INDEX idx_transaction_import_reference_batch ON transaction_import_reference(import_batch_id);
CREATE INDEX idx_transaction_import_reference_source_hash ON transaction_import_reference(source_hash);
CREATE INDEX idx_transaction_import_reference_source_operation ON transaction_import_reference(source_operation_id);
CREATE INDEX idx_transaction_import_reference_profile_account_source
    ON transaction_import_reference(profile_id, account_id, import_source);

WITH seed(parent_key, category_key, name, type, scope, default_movement_type, budgetable, technical) AS (
    VALUES
        (NULL, 'ingresos', 'Ingresos', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        (NULL, 'alimentacion', 'Alimentación', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'transporte', 'Transporte', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'servicios', 'Servicios', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'impuestos', 'Impuestos', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'segurosymutuales', 'Seguros y mutuales', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'bancosycomisiones', 'Bancos y comisiones', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'deudas', 'Deudas', 'DEBT', 'GLOBAL', 'ADJUSTMENT', TRUE, FALSE),
        (NULL, 'transferencias', 'Transferencias', 'SAVING', 'GLOBAL', 'TRANSFER', FALSE, TRUE),
        (NULL, 'familiaypersonales', 'Familia y personales', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'operacionestecnicas', 'Operaciones técnicas', 'VARIABLE_EXPENSE', 'GLOBAL', 'ADJUSTMENT', FALSE, TRUE),
        ('ingresos', 'sueldo', 'Sueldo', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresos', 'interesesyrendimientos', 'Intereses y rendimientos', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresos', 'rendimientomercadopago', 'Rendimiento Mercado Pago', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresos', 'transferenciasrecibidas', 'Transferencias recibidas', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresos', 'beneficiosypromociones', 'Beneficios y promociones', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresos', 'cobrosporlink', 'Cobros por link', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('alimentacion', 'supermercado', 'Supermercado', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('alimentacion', 'almacen', 'Almacén', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('alimentacion', 'deliveryyrestaurantes', 'Delivery y restaurantes', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('transporte', 'transportepublico', 'Transporte público', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('transporte', 'taxiyapps', 'Taxi y apps', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('transporte', 'combustible', 'Combustible', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('transporte', 'pasajes', 'Pasajes', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('servicios', 'electricidad', 'Electricidad', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('servicios', 'gas', 'Gas', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('servicios', 'telefoniamovil', 'Telefonía móvil', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('servicios', 'internet', 'Internet', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('servicios', 'plataformasdigitales', 'Plataformas digitales', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('servicios', 'meli', 'Meli+', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('impuestos', 'monotributo', 'Monotributo', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('impuestos', 'percepcionesrg4815', 'Percepciones RG 4815', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('impuestos', 'arcaafip', 'ARCA / AFIP', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('impuestos', 'impuestosbancarios', 'Impuestos bancarios', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('segurosymutuales', 'seguros', 'Seguros', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('segurosymutuales', 'mutual', 'Mutual', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('segurosymutuales', 'cobranzarecurrentearevisar', 'Cobranza recurrente a revisar', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('bancosycomisiones', 'comisionesbancarias', 'Comisiones bancarias', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('bancosycomisiones', 'puntoefectivo', 'Punto efectivo', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('bancosycomisiones', 'mantenimientodecuenta', 'Mantenimiento de cuenta', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('deudas', 'prestamobancoprovincia', 'Préstamo Banco Provincia', 'DEBT', 'GLOBAL', 'ADJUSTMENT', TRUE, FALSE),
        ('deudas', 'mercadocredito', 'Mercado Crédito', 'DEBT', 'GLOBAL', 'ADJUSTMENT', TRUE, FALSE),
        ('deudas', 'cuotasdeprestamo', 'Cuotas de préstamo', 'DEBT', 'GLOBAL', 'ADJUSTMENT', TRUE, FALSE),
        ('deudas', 'interesesyfinanciacion', 'Intereses y financiación', 'DEBT', 'GLOBAL', 'ADJUSTMENT', TRUE, FALSE),
        ('transferencias', 'transferenciasinternas', 'Transferencias internas', 'SAVING', 'GLOBAL', 'TRANSFER', FALSE, TRUE),
        ('transferencias', 'fondeomercadopago', 'Fondeo Mercado Pago', 'SAVING', 'GLOBAL', 'TRANSFER', FALSE, TRUE),
        ('transferencias', 'cuentadnidebin', 'Cuenta DNI / DEBIN', 'SAVING', 'GLOBAL', 'TRANSFER', FALSE, TRUE),
        ('transferencias', 'transferenciasaterceros', 'Transferencias a terceros', 'SAVING', 'GLOBAL', 'TRANSFER', FALSE, TRUE),
        ('transferencias', 'transferenciasrecibidasarevisar', 'Transferencias recibidas a revisar', 'SAVING', 'GLOBAL', 'TRANSFER', FALSE, TRUE),
        ('familiaypersonales', 'hijos', 'Hijos', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('familiaypersonales', 'compraspersonales', 'Compras personales', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('familiaypersonales', 'variosarevisar', 'Varios a revisar', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('operacionestecnicas', 'movimientoignorado', 'Movimiento ignorado', 'VARIABLE_EXPENSE', 'GLOBAL', 'ADJUSTMENT', FALSE, TRUE),
        ('operacionestecnicas', 'ajustedesaldo', 'Ajuste de saldo', 'VARIABLE_EXPENSE', 'GLOBAL', 'ADJUSTMENT', FALSE, TRUE),
        ('operacionestecnicas', 'diferenciaporredondeo', 'Diferencia por redondeo', 'VARIABLE_EXPENSE', 'GLOBAL', 'ADJUSTMENT', FALSE, TRUE),
        ('operacionestecnicas', 'ruidodeimportacion', 'Ruido de importación', 'VARIABLE_EXPENSE', 'GLOBAL', 'ADJUSTMENT', FALSE, TRUE)
),
roots AS (
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
        active,
        created_at,
        updated_at
    )
    SELECT
        NULL,
        NULL,
        name,
        category_key,
        type,
        scope,
        default_movement_type,
        budgetable,
        technical,
        TRUE,
        now(),
        now()
    FROM seed
    WHERE parent_key IS NULL
    RETURNING id, category_key, type
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
    active,
    created_at,
    updated_at
)
SELECT
    NULL,
    parent.id,
    child.name,
    child.category_key,
    child.type,
    child.scope,
    child.default_movement_type,
    child.budgetable,
    child.technical,
    TRUE,
    now(),
    now()
FROM seed child
JOIN roots parent
  ON parent.category_key = child.parent_key
 AND parent.type = child.type
WHERE child.parent_key IS NOT NULL;

WITH alias_seed(source, alias_raw, alias_normalized, canonical_name, category_key, payment_channel, confidence) AS (
    VALUES
        ('BANCO_PROVINCIA', 'PAYU*AR*UBER', 'payu ar uber', 'Uber', 'taxiyapps', 'DEBIT_CARD', 0.95),
        ('BANCO_PROVINCIA', 'MERPAGO*SUBE', 'merpago sube', 'SUBE', 'transportepublico', 'TRANSPORT_CARD', 0.95),
        ('BANCO_PROVINCIA', 'BUSPLUS', 'busplus', 'BUSPLUS', 'transportepublico', 'TRANSPORT_CARD', 0.95),
        ('BANCO_PROVINCIA', 'DIA TIENDA', 'dia tienda', 'DIA', 'supermercado', 'DEBIT_CARD', 0.95),
        ('BANCO_PROVINCIA', 'MERPAGO*SUPERDIA', 'merpago superdia', 'DIA', 'supermercado', 'DEBIT_CARD', 0.95),
        ('BANCO_PROVINCIA', 'PEDIDOSYA', 'pedidosya', 'PedidosYa', 'deliveryyrestaurantes', 'DEBIT_CARD', 0.95),
        ('BANCO_PROVINCIA', 'MERPAGO*MOSTAZA', 'merpago mostaza', 'Mostaza', 'deliveryyrestaurantes', 'DEBIT_CARD', 0.95),
        ('BANCO_PROVINCIA', 'MERPAGO*TUENTI', 'merpago tuenti', 'Tuenti', 'telefoniamovil', 'DEBIT_CARD', 0.95),
        ('MERCADO_PAGO', 'EDEA', 'edea', 'EDEA', 'electricidad', 'MERCADO_PAGO', 0.95),
        ('MERCADO_PAGO', 'CAMUZZI', 'camuzzi', 'Camuzzi', 'gas', 'MERCADO_PAGO', 0.95),
        ('MERCADO_PAGO', 'SHELL BOX', 'shell box', 'Shell Box', 'combustible', 'MERCADO_PAGO', 0.95),
        ('MERCADO_PAGO', 'MELI+', 'meli', 'Meli+', 'meli', 'MERCADO_PAGO', 0.95),
        ('MERCADO_PAGO', 'MERCADO CREDITO', 'mercado credito', 'Mercado Crédito', 'mercadocredito', 'MERCADO_CREDITO', 0.95),
        ('MERCADO_PAGO', 'MERCADOCRÉDITO', 'mercadocredito', 'Mercado Crédito', 'mercadocredito', 'MERCADO_CREDITO', 0.95),
        ('MERCADO_PAGO', 'EMOVA', 'emova', 'EMOVA', 'transportepublico', 'TRANSPORT_CARD', 0.95)
)
INSERT INTO merchant_alias (
    profile_id,
    source,
    alias_raw,
    alias_normalized,
    canonical_name,
    category_id,
    payment_channel,
    confidence,
    active,
    created_at,
    updated_at
)
SELECT
    NULL,
    alias_seed.source,
    alias_seed.alias_raw,
    alias_seed.alias_normalized,
    alias_seed.canonical_name,
    category.id,
    alias_seed.payment_channel,
    alias_seed.confidence,
    TRUE,
    now(),
    now()
FROM alias_seed
JOIN category
  ON category.profile_id IS NULL
 AND category.category_key = alias_seed.category_key
 AND category.active = TRUE;

WITH rule_seed(priority, source, field_name, pattern, pattern_type, category_key, movement_type, payment_channel, classification_status, confidence, reason_code, warning) AS (
    VALUES
        (10, 'MERCADO_PAGO', 'rawDescription', 'EMPTY_DETAIL_POSITIVE_UNLIQUIDATED', 'EXACT', 'rendimientomercadopago', 'INCOME', 'MONEY_MARKET_YIELD', 'CLASSIFIED', 0.95, 'RULE_MP_YIELD_EMPTY_DETAIL', NULL),
        (20, 'MERCADO_PAGO', 'identificationNumber', 'EMOVA', 'CONTAINS', 'transportepublico', 'EXPENSE', 'TRANSPORT_CARD', 'CLASSIFIED', 0.95, 'RULE_MP_EMOVA_TRANSPORT', NULL),
        (100, 'BANCO_PROVINCIA', 'merchant', 'PAYU*AR*UBER', 'CONTAINS', 'taxiyapps', 'EXPENSE', 'DEBIT_CARD', 'CLASSIFIED', 0.95, 'RULE_MERCHANT_UBER', NULL),
        (110, 'BANCO_PROVINCIA', 'merchant', 'MERPAGO*SUBE', 'CONTAINS', 'transportepublico', 'EXPENSE', 'TRANSPORT_CARD', 'CLASSIFIED', 0.95, 'RULE_MERCHANT_SUBE', NULL),
        (130, 'BANCO_PROVINCIA', 'merchant', 'DIA TIENDA', 'CONTAINS', 'supermercado', 'EXPENSE', 'DEBIT_CARD', 'CLASSIFIED', 0.95, 'RULE_MERCHANT_DIA', NULL),
        (140, 'BANCO_PROVINCIA', 'merchant', 'PEDIDOSYA', 'CONTAINS', 'deliveryyrestaurantes', 'EXPENSE', 'DEBIT_CARD', 'CLASSIFIED', 0.95, 'RULE_MERCHANT_PEDIDOSYA', NULL),
        (200, 'BANCO_PROVINCIA', 'extendedDescription', 'ARCA|AFIP|MONOTRIB', 'REGEX', 'monotributo', 'EXPENSE', 'DIRECT_DEBIT', 'CLASSIFIED', 0.95, 'RULE_ARCA_AFIP_MONOTRIBUTO', NULL),
        (210, 'BANCO_PROVINCIA', 'extendedDescription', 'CAJA DE SEG', 'CONTAINS', 'seguros', 'EXPENSE', 'DIRECT_DEBIT', 'CLASSIFIED', 0.95, 'RULE_DIRECT_DEBIT_INSURANCE', NULL),
        (220, 'BANCO_PROVINCIA', 'extendedDescription', 'BK COBRANZAS|TERTIUM SA|ASOCIACION MUTU', 'REGEX', 'cobranzarecurrentearevisar', 'EXPENSE', 'DIRECT_DEBIT', 'REVIEW', 0.70, 'RULE_DIRECT_DEBIT_RECURRENT_REVIEW', 'Débito recurrente sensible: requiere alias de perfil para autoclasificar.'),
        (300, 'MERCADO_PAGO', 'normalizedDescription', 'PAGO DEBIN|BANK TRANSFER|TRANSFERENCIA BANCARIA', 'REGEX', 'fondeomercadopago', 'TRANSFER', 'BANK_TRANSFER', 'TECHNICAL', 0.70, 'RULE_MP_FUNDING_TRANSFER', 'No impacta como ingreso operativo.'),
        (310, 'MERCADO_PAGO', 'rawDescription', 'Link de pago', 'CONTAINS', 'cobrosporlink', 'INCOME', 'MERCADO_PAGO', 'REVIEW', 0.70, 'RULE_MP_PAYMENT_LINK_REVIEW', 'Puede ser recupero, venta o transferencia.'),
        (900, 'BANCO_PROVINCIA', 'rawDescription', 'PAGO CON TARJETA DEBITO', 'CONTAINS', NULL, 'EXPENSE', 'DEBIT_CARD', 'REVIEW', 0.35, 'RULE_DEBIT_CARD_PURCHASE_REVIEW', 'Fallback genérico: solo corre después de aliases/reglas específicas.'),
        (920, 'BANCO_PROVINCIA', 'rawDescription', 'DEBITO PAGO DIRECTO', 'CONTAINS', NULL, 'EXPENSE', 'DIRECT_DEBIT', 'REVIEW', 0.35, 'RULE_DIRECT_DEBIT_REVIEW', 'Fallback genérico: solo corre después de reglas específicas.')
)
INSERT INTO classification_rule (
    profile_id,
    priority,
    source,
    field_name,
    pattern,
    pattern_type,
    category_id,
    movement_type,
    payment_channel,
    classification_status,
    confidence,
    reason_code,
    warning,
    active,
    created_at,
    updated_at
)
SELECT
    NULL,
    rule_seed.priority,
    rule_seed.source,
    rule_seed.field_name,
    rule_seed.pattern,
    rule_seed.pattern_type,
    category.id,
    rule_seed.movement_type,
    rule_seed.payment_channel,
    rule_seed.classification_status,
    rule_seed.confidence,
    rule_seed.reason_code,
    rule_seed.warning,
    TRUE,
    now(),
    now()
FROM rule_seed
LEFT JOIN category
  ON category.profile_id IS NULL
 AND category.category_key = rule_seed.category_key
 AND category.active = TRUE;
