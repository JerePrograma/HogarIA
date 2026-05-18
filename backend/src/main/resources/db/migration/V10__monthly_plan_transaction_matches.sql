CREATE TABLE IF NOT EXISTS monthly_plan_transaction_match (
  id UUID PRIMARY KEY,
  profile_id UUID NOT NULL,
  monthly_plan_item_id UUID NOT NULL,
  money_transaction_id UUID NOT NULL,
  matched_amount NUMERIC(19,2) NOT NULL,
  match_type VARCHAR(40) NOT NULL,
  confidence VARCHAR(20) NOT NULL,
  note VARCHAR(500),
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_monthly_plan_tx_match_item FOREIGN KEY (monthly_plan_item_id) REFERENCES monthly_plan_item(id),
  CONSTRAINT fk_monthly_plan_tx_match_tx FOREIGN KEY (money_transaction_id) REFERENCES money_transaction(id),
  CONSTRAINT uq_monthly_plan_tx_match_pair UNIQUE (profile_id, monthly_plan_item_id, money_transaction_id)
);

CREATE INDEX IF NOT EXISTS idx_monthly_plan_tx_match_profile ON monthly_plan_transaction_match(profile_id);
CREATE INDEX IF NOT EXISTS idx_monthly_plan_tx_match_item ON monthly_plan_transaction_match(monthly_plan_item_id);
CREATE INDEX IF NOT EXISTS idx_monthly_plan_tx_match_tx ON monthly_plan_transaction_match(money_transaction_id);

INSERT INTO monthly_plan_transaction_match (
  id, profile_id, monthly_plan_item_id, money_transaction_id, matched_amount, match_type, confidence, note, created_at, updated_at
)
SELECT
  gen_random_uuid(),
  i.profile_id,
  i.id,
  i.transaction_id,
  COALESCE(i.amount, 0),
  'SYSTEM_CONVERSION',
  'HIGH',
  'Backfill from monthly_plan_item.transaction_id',
  NOW(),
  NOW()
FROM monthly_plan_item i
WHERE i.transaction_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM monthly_plan_transaction_match m
    WHERE m.profile_id = i.profile_id
      AND m.monthly_plan_item_id = i.id
      AND m.money_transaction_id = i.transaction_id
  );
