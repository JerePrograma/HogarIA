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
    raw_payload JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_transaction_import_reference_transaction FOREIGN KEY (transaction_id) REFERENCES money_transaction(id) ON DELETE CASCADE,
    CONSTRAINT fk_transaction_import_reference_batch FOREIGN KEY (import_batch_id) REFERENCES excel_import_batch(id) ON DELETE SET NULL,
    CONSTRAINT fk_transaction_import_reference_row FOREIGN KEY (import_row_id) REFERENCES excel_import_row(id) ON DELETE SET NULL
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
CREATE INDEX idx_category_profile_budgetable
    ON category(profile_id, budgetable, technical);
CREATE INDEX idx_category_profile_budgetable_technical
    ON category(profile_id, budgetable, technical);
CREATE INDEX idx_category_global_category_key
    ON category(category_key)
    WHERE profile_id IS NULL;

CREATE INDEX idx_money_tx_profile_budget_date ON money_transaction(profile_id, budget_date);
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
    WHERE source_hash IS NOT NULL;
CREATE INDEX idx_money_tx_internal_transfer_group
    ON money_transaction(internal_transfer_group_id)
    WHERE internal_transfer_group_id IS NOT NULL;
CREATE INDEX idx_money_tx_profile_operation_datetime ON money_transaction(profile_id, operation_datetime);
CREATE INDEX idx_money_tx_profile_duplicate_fingerprint
    ON money_transaction(profile_id, account_id, duplicate_fingerprint)
    WHERE duplicate_fingerprint IS NOT NULL;
CREATE UNIQUE INDEX ux_money_tx_profile_source_operation_idempotent
    ON money_transaction(profile_id, source, source_operation_id)
    WHERE source IS NOT NULL AND source_operation_id IS NOT NULL AND status <> 'IGNORED';
CREATE UNIQUE INDEX ux_money_tx_profile_account_duplicate_fingerprint_active
    ON money_transaction(profile_id, account_id, duplicate_fingerprint)
    WHERE duplicate_fingerprint IS NOT NULL
      AND status <> 'IGNORED'
      AND classification_status <> 'IGNORED_BY_RULE';
CREATE UNIQUE INDEX ux_money_tx_profile_source_operation_strict
    ON money_transaction(profile_id, source, source_operation_id)
    WHERE source IS NOT NULL AND source_operation_id IS NOT NULL;

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
CREATE UNIQUE INDEX ux_transaction_import_reference_source_hash
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
        (NULL, 'ingresoslaborales', 'Ingresos laborales', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        (NULL, 'ingresosprofesionales', 'Ingresos profesionales', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        (NULL, 'ingresosdenegocio', 'Ingresos de negocio', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        (NULL, 'ingresosfinancieros', 'Ingresos financieros', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        (NULL, 'ingresoseventuales', 'Ingresos eventuales', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        (NULL, 'cjingresosdeprestamos', 'CJ - Ingresos de préstamos', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        (NULL, 'vivienda', 'Vivienda', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'servicios', 'Servicios', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'saludfija', 'Salud fija', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'educacionfija', 'Educación fija', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'seguros', 'Seguros', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'suscripcionesfijas', 'Suscripciones fijas', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'impuestosfijos', 'Impuestos fijos', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'alimentacion', 'Alimentación', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'transporte', 'Transporte', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'hogar', 'Hogar', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'saludvariable', 'Salud variable', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'familiaehijos', 'Familia e hijos', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'mascotas', 'Mascotas', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'indumentaria', 'Indumentaria', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'ocio', 'Ocio', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'viajes', 'Viajes', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'bancosycomisiones', 'Bancos y comisiones', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'impuestosvariables', 'Impuestos variables', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'gastosgenerales', 'Gastos generales', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'ahorro', 'Ahorro', 'SAVING', 'GLOBAL', 'SAVING', TRUE, FALSE),
        (NULL, 'transferenciasinternas', 'Transferencias internas', 'SAVING', 'GLOBAL', 'TRANSFER', FALSE, TRUE),
        (NULL, 'inversiones', 'Inversiones', 'INVESTMENT', 'GLOBAL', 'SAVING', TRUE, FALSE),
        (NULL, 'cjprestamosotorgados', 'CJ - Préstamos otorgados', 'INVESTMENT', 'GLOBAL', 'SAVING', TRUE, FALSE),
        (NULL, 'deudas', 'Deudas', 'DEBT', 'GLOBAL', 'ADJUSTMENT', TRUE, FALSE),
        (NULL, 'operacionestecnicas', 'Operaciones técnicas', 'VARIABLE_EXPENSE', 'GLOBAL', 'ADJUSTMENT', FALSE, TRUE),
        ('ingresoslaborales', 'sueldo', 'Sueldo', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresoslaborales', 'aguinaldo', 'Aguinaldo', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresoslaborales', 'bonosycomisiones', 'Bonos y comisiones', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresoslaborales', 'horasextra', 'Horas extra', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresosprofesionales', 'honorarios', 'Honorarios', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresosprofesionales', 'freelance', 'Freelance', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresosprofesionales', 'serviciosprofesionales', 'Servicios profesionales', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresosdenegocio', 'ventas', 'Ventas', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresosdenegocio', 'cobrosdeclientes', 'Cobros de clientes', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresosdenegocio', 'recuperodegastos', 'Recupero de gastos', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresosfinancieros', 'interesesganados', 'Intereses ganados', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresosfinancieros', 'rendimientos', 'Rendimientos', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresosfinancieros', 'dividendos', 'Dividendos', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresoseventuales', 'beneficiosypromociones', 'Beneficios y promociones', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresoseventuales', 'devolucionesyreintegros', 'Devoluciones y reintegros', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresoseventuales', 'transferenciasrecibidas', 'Transferencias recibidas', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresoseventuales', 'ayudafamiliar', 'Ayuda familiar', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresoseventuales', 'ventadeusados', 'Venta de usados', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresoseventuales', 'otrosingresos', 'Otros ingresos', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('cjingresosdeprestamos', 'cjinterescobrado', 'CJ - Interés cobrado', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('cjingresosdeprestamos', 'cjmoracobrada', 'CJ - Mora cobrada', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('cjingresosdeprestamos', 'cjcomisioncobrada', 'CJ - Comisión cobrada', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('vivienda', 'alquiler', 'Alquiler', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('vivienda', 'expensas', 'Expensas', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('vivienda', 'hipoteca', 'Hipoteca', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('vivienda', 'abl', 'ABL', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('vivienda', 'impuestoinmobiliario', 'Impuesto inmobiliario', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('servicios', 'electricidad', 'Electricidad', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('servicios', 'gas', 'Gas', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('servicios', 'agua', 'Agua', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('servicios', 'internet', 'Internet', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('servicios', 'telefoniamovil', 'Telefonía móvil', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('servicios', 'cable', 'Cable', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('servicios', 'streaming', 'Streaming', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('saludfija', 'prepaga', 'Prepaga', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('saludfija', 'obrasocial', 'Obra social', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('saludfija', 'planmedico', 'Plan médico', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('educacionfija', 'cuotaescolar', 'Cuota escolar', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('educacionfija', 'universidad', 'Universidad', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('educacionfija', 'cursosrecurrentes', 'Cursos recurrentes', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('seguros', 'segurodelauto', 'Seguro del auto', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('seguros', 'segurodelhogar', 'Seguro del hogar', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('seguros', 'segurodevida', 'Seguro de vida', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('seguros', 'segurodecaucion', 'Seguro de caución', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('suscripcionesfijas', 'software', 'Software', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('suscripcionesfijas', 'gimnasio', 'Gimnasio', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('suscripcionesfijas', 'plataformasdigitales', 'Plataformas digitales', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('suscripcionesfijas', 'herramientasdetrabajo', 'Herramientas de trabajo', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('impuestosfijos', 'monotributo', 'Monotributo', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('impuestosfijos', 'autonomos', 'Autónomos', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('impuestosfijos', 'ingresosbrutos', 'Ingresos Brutos', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('impuestosfijos', 'iva', 'IVA', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('impuestosfijos', 'ganancias', 'Ganancias', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('impuestosfijos', 'patentes', 'Patentes', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('alimentacion', 'supermercado', 'Supermercado', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('alimentacion', 'almacen', 'Almacén', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('alimentacion', 'verduleria', 'Verdulería', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('alimentacion', 'carniceria', 'Carnicería', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('alimentacion', 'delivery', 'Delivery', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('alimentacion', 'restaurantes', 'Restaurantes', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('transporte', 'transportepublico', 'Transporte público', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('transporte', 'combustible', 'Combustible', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('transporte', 'taxiyapps', 'Taxi y apps', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('transporte', 'estacionamiento', 'Estacionamiento', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('transporte', 'peajes', 'Peajes', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('transporte', 'mantenimientovehicular', 'Mantenimiento vehicular', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('hogar', 'limpieza', 'Limpieza', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('hogar', 'reparaciones', 'Reparaciones', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('hogar', 'ferreteria', 'Ferretería', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('hogar', 'mueblesydecoracion', 'Muebles y decoración', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('hogar', 'electrodomesticos', 'Electrodomésticos', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('saludvariable', 'medicamentos', 'Medicamentos', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('saludvariable', 'consultasmedicas', 'Consultas médicas', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('saludvariable', 'estudiosmedicos', 'Estudios médicos', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('saludvariable', 'odontologia', 'Odontología', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('saludvariable', 'terapia', 'Terapia', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('familiaehijos', 'colegioymateriales', 'Colegio y materiales', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('familiaehijos', 'actividadesdehijos', 'Actividades de hijos', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('familiaehijos', 'ropadehijos', 'Ropa de hijos', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('familiaehijos', 'juguetes', 'Juguetes', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('familiaehijos', 'cumpleanosyeventos', 'Cumpleaños y eventos', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('mascotas', 'alimentodemascotas', 'Alimento de mascotas', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('mascotas', 'veterinaria', 'Veterinaria', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('mascotas', 'accesoriosdemascotas', 'Accesorios de mascotas', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('indumentaria', 'ropa', 'Ropa', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('indumentaria', 'calzado', 'Calzado', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('indumentaria', 'accesorios', 'Accesorios', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('ocio', 'salidas', 'Salidas', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('ocio', 'entretenimiento', 'Entretenimiento', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('ocio', 'regalos', 'Regalos', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('ocio', 'donaciones', 'Donaciones', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('viajes', 'pasajes', 'Pasajes', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('viajes', 'hospedaje', 'Hospedaje', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('viajes', 'excursiones', 'Excursiones', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('viajes', 'viaticos', 'Viáticos', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('bancosycomisiones', 'comisionesycargos', 'Comisiones y cargos', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('bancosycomisiones', 'comisionesbancarias', 'Comisiones bancarias', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('bancosycomisiones', 'mantenimientodecuenta', 'Mantenimiento de cuenta', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('bancosycomisiones', 'impuestodebitosycreditos', 'Impuesto débitos y créditos', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('bancosycomisiones', 'interesesporfinanciacion', 'Intereses por financiación', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('impuestosvariables', 'percepciones', 'Percepciones', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('impuestosvariables', 'retenciones', 'Retenciones', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('impuestosvariables', 'arca', 'ARCA', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('impuestosvariables', 'multasyrecargos', 'Multas y recargos', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('gastosgenerales', 'comprasvarias', 'Compras varias', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('gastosgenerales', 'gastosmenores', 'Gastos menores', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('gastosgenerales', 'otrosgastos', 'Otros gastos', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('ahorro', 'fondodeemergencia', 'Fondo de emergencia', 'SAVING', 'GLOBAL', 'SAVING', TRUE, FALSE),
        ('ahorro', 'ahorrogeneral', 'Ahorro general', 'SAVING', 'GLOBAL', 'SAVING', TRUE, FALSE),
        ('ahorro', 'objetivovacaciones', 'Objetivo vacaciones', 'SAVING', 'GLOBAL', 'SAVING', TRUE, FALSE),
        ('ahorro', 'objetivoauto', 'Objetivo auto', 'SAVING', 'GLOBAL', 'SAVING', TRUE, FALSE),
        ('ahorro', 'objetivovivienda', 'Objetivo vivienda', 'SAVING', 'GLOBAL', 'SAVING', TRUE, FALSE),
        ('ahorro', 'educacionfutura', 'Educación futura', 'SAVING', 'GLOBAL', 'SAVING', TRUE, FALSE),
        ('transferenciasinternas', 'cuentadnidebin', 'Cuenta DNI / DEBIN', 'SAVING', 'GLOBAL', 'TRANSFER', FALSE, TRUE),
        ('transferenciasinternas', 'fondeomercadopagotransferenciasinternas', 'Fondeo MercadoPago / transferencias internas', 'SAVING', 'GLOBAL', 'TRANSFER', FALSE, TRUE),
        ('transferenciasinternas', 'transferenciaentrecuentas', 'Transferencia entre cuentas', 'SAVING', 'GLOBAL', 'TRANSFER', FALSE, TRUE),
        ('transferenciasinternas', 'movimientoentrebilleteras', 'Movimiento entre billeteras', 'SAVING', 'GLOBAL', 'TRANSFER', FALSE, TRUE),
        ('inversiones', 'dolares', 'Dólares', 'INVESTMENT', 'GLOBAL', 'SAVING', TRUE, FALSE),
        ('inversiones', 'plazofijo', 'Plazo fijo', 'INVESTMENT', 'GLOBAL', 'SAVING', TRUE, FALSE),
        ('inversiones', 'fondoscomunes', 'Fondos comunes', 'INVESTMENT', 'GLOBAL', 'SAVING', TRUE, FALSE),
        ('inversiones', 'cedears', 'CEDEARs', 'INVESTMENT', 'GLOBAL', 'SAVING', TRUE, FALSE),
        ('inversiones', 'acciones', 'Acciones', 'INVESTMENT', 'GLOBAL', 'SAVING', TRUE, FALSE),
        ('inversiones', 'bonos', 'Bonos', 'INVESTMENT', 'GLOBAL', 'SAVING', TRUE, FALSE),
        ('inversiones', 'cripto', 'Cripto', 'INVESTMENT', 'GLOBAL', 'SAVING', TRUE, FALSE),
        ('inversiones', 'rendimientosreinvertidos', 'Rendimientos reinvertidos', 'INVESTMENT', 'GLOBAL', 'SAVING', TRUE, FALSE),
        ('cjprestamosotorgados', 'cjcapitalprestado', 'CJ - Capital prestado', 'INVESTMENT', 'GLOBAL', 'ADJUSTMENT', FALSE, FALSE),
        ('cjprestamosotorgados', 'cjcapitalrecuperado', 'CJ - Capital recuperado', 'INVESTMENT', 'GLOBAL', 'SAVING', FALSE, FALSE),
        ('cjprestamosotorgados', 'cjajustedeprestamo', 'CJ - Ajuste de préstamo', 'INVESTMENT', 'GLOBAL', 'ADJUSTMENT', TRUE, FALSE),
        ('deudas', 'tarjetadecredito', 'Tarjeta de crédito', 'DEBT', 'GLOBAL', 'ADJUSTMENT', TRUE, FALSE),
        ('deudas', 'creditosyfinanciacion', 'Créditos y financiación', 'DEBT', 'GLOBAL', 'ADJUSTMENT', TRUE, FALSE),
        ('deudas', 'prestamopersonal', 'Préstamo personal', 'DEBT', 'GLOBAL', 'ADJUSTMENT', TRUE, FALSE),
        ('deudas', 'prestamofamiliar', 'Préstamo familiar', 'DEBT', 'GLOBAL', 'ADJUSTMENT', TRUE, FALSE),
        ('deudas', 'mercadocredito', 'Mercado Crédito', 'DEBT', 'GLOBAL', 'ADJUSTMENT', TRUE, FALSE),
        ('deudas', 'cuotaspendientes', 'Cuotas pendientes', 'DEBT', 'GLOBAL', 'ADJUSTMENT', TRUE, FALSE),
        ('deudas', 'interesesymora', 'Intereses y mora', 'DEBT', 'GLOBAL', 'ADJUSTMENT', TRUE, FALSE),
        ('deudas', 'refinanciacion', 'Refinanciación', 'DEBT', 'GLOBAL', 'ADJUSTMENT', TRUE, FALSE),
        ('deudas', 'cancelaciondedeuda', 'Cancelación de deuda', 'DEBT', 'GLOBAL', 'ADJUSTMENT', TRUE, FALSE),
        ('operacionestecnicas', 'ajustedesaldo', 'Ajuste de saldo', 'VARIABLE_EXPENSE', 'GLOBAL', 'ADJUSTMENT', FALSE, TRUE),
        ('operacionestecnicas', 'diferenciaporredondeo', 'Diferencia por redondeo', 'VARIABLE_EXPENSE', 'GLOBAL', 'ADJUSTMENT', FALSE, TRUE),
        ('operacionestecnicas', 'movimientoignorado', 'Movimiento ignorado', 'VARIABLE_EXPENSE', 'GLOBAL', 'ADJUSTMENT', FALSE, TRUE),
        ('operacionestecnicas', 'reclasificacionmanual', 'Reclasificación manual', 'VARIABLE_EXPENSE', 'GLOBAL', 'ADJUSTMENT', FALSE, TRUE)
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
