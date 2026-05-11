CREATE TABLE external_sync_mapping (
    id UUID PRIMARY KEY,
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
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_external_sync_mapping_profile FOREIGN KEY (profile_id) REFERENCES financial_profile(id),
    CONSTRAINT fk_external_sync_mapping_money_transaction FOREIGN KEY (money_transaction_id) REFERENCES money_transaction(id),
    CONSTRAINT fk_external_sync_mapping_monthly_plan_item FOREIGN KEY (monthly_plan_item_id) REFERENCES monthly_plan_item(id),
    CONSTRAINT uk_external_sync_mapping_external_event UNIQUE (external_system, external_entity_type, external_entity_id, external_event_type)
);

CREATE INDEX idx_external_sync_mapping_profile ON external_sync_mapping(profile_id);
CREATE INDEX idx_external_sync_mapping_status ON external_sync_mapping(status);
