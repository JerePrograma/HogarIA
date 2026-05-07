CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE TABLE app_user (id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
email VARCHAR(120) UNIQUE NOT NULL,password_hash VARCHAR(255) NOT NULL,full_name VARCHAR(120) NOT NULL,created_at TIMESTAMP NOT NULL DEFAULT now());

CREATE TABLE financial_profile (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 user_id UUID NOT NULL REFERENCES app_user(id),
 name VARCHAR(100) NOT NULL,
 type VARCHAR(20) NOT NULL CHECK (type IN ('PERSONAL','FAMILY','BUSINESS')),
 base_currency VARCHAR(3) NOT NULL,
 active_year INT NOT NULL,
 UNIQUE(user_id, name)
);

CREATE TABLE account (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 profile_id UUID NOT NULL REFERENCES financial_profile(id),
 name VARCHAR(120) NOT NULL,
 account_type VARCHAR(30) NOT NULL CHECK (account_type IN ('CASH','BANK','CREDIT_CARD','DEBIT_CARD','VIRTUAL_WALLET','BUSINESS')),
 currency VARCHAR(3) NOT NULL,
 credit_limit NUMERIC(19,2),
 statement_close_day INT,
 due_day INT,
 active BOOLEAN NOT NULL DEFAULT TRUE,
 UNIQUE(profile_id, name)
);

CREATE TABLE category (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
profile_id UUID REFERENCES financial_profile(id),
parent_id UUID REFERENCES category(id),
name VARCHAR(120) NOT NULL,
type VARCHAR(30) NOT NULL CHECK (type IN ('INCOME','FIXED_EXPENSE','VARIABLE_EXPENSE','SAVING','DEBT','INVESTMENT')),
scope VARCHAR(20) NOT NULL CHECK (scope IN ('PERSONAL','FAMILY','BUSINESS','GLOBAL')),
active BOOLEAN NOT NULL DEFAULT TRUE,
UNIQUE(profile_id, name, type)
);

CREATE TABLE money_transaction (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 profile_id UUID NOT NULL REFERENCES financial_profile(id),
 account_id UUID NOT NULL REFERENCES account(id),
 category_id UUID NOT NULL REFERENCES category(id),
 movement_type VARCHAR(20) NOT NULL CHECK (movement_type IN ('INCOME','EXPENSE','SAVING','TRANSFER','ADJUSTMENT')),
 real_date DATE NOT NULL,
 budget_date DATE NOT NULL,
 amount NUMERIC(19,2) NOT NULL CHECK(amount > 0),
 currency VARCHAR(3) NOT NULL,
 description VARCHAR(255),
 origin VARCHAR(20) NOT NULL DEFAULT 'MANUAL' CHECK (origin IN ('MANUAL','IMPORT','RECURRENT','SYSTEM')),
 status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED' CHECK (status IN ('CONFIRMED','PENDING','IGNORED')),
 created_at TIMESTAMP NOT NULL DEFAULT now(),
 updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_profile_user_id ON financial_profile(user_id);
CREATE INDEX idx_account_profile_id ON account(profile_id);
CREATE INDEX idx_category_profile_id ON category(profile_id);
CREATE INDEX idx_money_tx_profile_budget_date ON money_transaction(profile_id, budget_date);
CREATE INDEX idx_money_tx_profile_category ON money_transaction(profile_id, category_id);
CREATE INDEX idx_money_tx_profile_account ON money_transaction(profile_id, account_id);
CREATE INDEX idx_money_tx_type ON money_transaction(movement_type);
CREATE INDEX idx_money_tx_status ON money_transaction(status);