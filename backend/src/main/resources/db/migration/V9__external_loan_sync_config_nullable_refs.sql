ALTER TABLE external_loan_sync_config
ALTER COLUMN account_id DROP NOT NULL,
ALTER COLUMN loan_disbursement_category_id DROP NOT NULL,
ALTER COLUMN principal_recovery_category_id DROP NOT NULL,
ALTER COLUMN interest_income_category_id DROP NOT NULL;
