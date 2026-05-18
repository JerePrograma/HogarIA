CREATE TABLE monthly_plan_transaction_match (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL REFERENCES financial_profile(id),
    monthly_plan_item_id UUID NOT NULL REFERENCES monthly_plan_item(id) ON DELETE CASCADE,
    money_transaction_id UUID NOT NULL REFERENCES money_transaction(id) ON DELETE CASCADE,
    matched_amount NUMERIC(19,2) NOT NULL CHECK (matched_amount > 0),
    match_type VARCHAR(30) NOT NULL DEFAULT 'MANUAL' CHECK (match_type IN ('MANUAL','SUGGESTED_ACCEPTED','AUTO','SYSTEM_CONVERSION')),
    confidence VARCHAR(20) NOT NULL DEFAULT 'HIGH' CHECK (confidence IN ('HIGH','MEDIUM','LOW')),
    note VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (profile_id, monthly_plan_item_id, money_transaction_id)
);

CREATE INDEX idx_monthly_plan_match_profile_item ON monthly_plan_transaction_match(profile_id, monthly_plan_item_id);
CREATE INDEX idx_monthly_plan_match_profile_tx ON monthly_plan_transaction_match(profile_id, money_transaction_id);
CREATE INDEX idx_monthly_plan_match_item ON monthly_plan_transaction_match(monthly_plan_item_id);
CREATE INDEX idx_monthly_plan_match_tx ON monthly_plan_transaction_match(money_transaction_id);
