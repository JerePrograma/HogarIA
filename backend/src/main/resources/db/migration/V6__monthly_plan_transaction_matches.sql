CREATE TABLE monthly_plan_transaction_match (
  id UUID PRIMARY KEY,
  profile_id UUID NOT NULL,
  monthly_plan_item_id UUID NOT NULL,
  money_transaction_id UUID NOT NULL,
  matched_amount NUMERIC(19,2) NOT NULL,
  match_type VARCHAR(40) NOT NULL,
  confidence VARCHAR(20) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  CONSTRAINT uq_monthly_plan_tx_pair UNIQUE (monthly_plan_item_id, money_transaction_id)
);

CREATE INDEX idx_monthly_plan_tx_match_profile ON monthly_plan_transaction_match(profile_id);
CREATE INDEX idx_monthly_plan_tx_match_item ON monthly_plan_transaction_match(monthly_plan_item_id);
CREATE INDEX idx_monthly_plan_tx_match_tx ON monthly_plan_transaction_match(money_transaction_id);
