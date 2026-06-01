CREATE UNIQUE INDEX IF NOT EXISTS ux_money_tx_profile_source_operation_strict
    ON money_transaction(profile_id, source, source_operation_id)
    WHERE source IS NOT NULL
      AND source_operation_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_money_tx_profile_source_hash
    ON money_transaction(profile_id, source_hash)
    WHERE source_hash IS NOT NULL;
