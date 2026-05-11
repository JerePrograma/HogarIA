ALTER TABLE external_sync_mapping
    DROP CONSTRAINT uk_external_sync_mapping_external_event;

ALTER TABLE external_sync_mapping
    ADD CONSTRAINT uk_external_sync_mapping_external_event_profile
    UNIQUE (profile_id, external_system, external_entity_type, external_entity_id, external_event_type);
