CREATE INDEX IF NOT EXISTS idx_monthly_plan_item_profile_transaction
    ON monthly_plan_item(profile_id, transaction_id)
    WHERE transaction_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_monthly_plan_tx_match_profile_transaction
    ON monthly_plan_transaction_match(profile_id, money_transaction_id);

CREATE INDEX IF NOT EXISTS idx_external_sync_mapping_profile_transaction
    ON external_sync_mapping(profile_id, money_transaction_id)
    WHERE money_transaction_id IS NOT NULL;
