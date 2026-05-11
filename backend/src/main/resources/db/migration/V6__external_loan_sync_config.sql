CREATE TABLE external_loan_sync_config (
    id UUID PRIMARY KEY,
    profile_id UUID NOT NULL,
    account_id UUID NOT NULL,
    loan_disbursement_category_id UUID NOT NULL,
    principal_recovery_category_id UUID NOT NULL,
    interest_income_category_id UUID NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_external_loan_sync_config_profile FOREIGN KEY (profile_id) REFERENCES financial_profile(id),
    CONSTRAINT fk_external_loan_sync_config_account FOREIGN KEY (account_id) REFERENCES account(id),
    CONSTRAINT fk_external_loan_sync_config_loan_disbursement_category FOREIGN KEY (loan_disbursement_category_id) REFERENCES category(id),
    CONSTRAINT fk_external_loan_sync_config_principal_recovery_category FOREIGN KEY (principal_recovery_category_id) REFERENCES category(id),
    CONSTRAINT fk_external_loan_sync_config_interest_income_category FOREIGN KEY (interest_income_category_id) REFERENCES category(id),
    CONSTRAINT uk_external_loan_sync_config_profile UNIQUE (profile_id)
);
