-- CJPrestamos idempotency preflight diagnostics.
-- Read-only script for manual use before/after Flyway migrations; do not wire as a migration.

-- 1) Duplicated (profile_id, source, source_operation_id) rows that would block the strict unique index.
SELECT
    profile_id,
    source,
    source_operation_id,
    COUNT(*) AS transaction_count,
    ARRAY_AGG(id ORDER BY created_at, id) AS transaction_ids,
    STRING_AGG(LEFT(COALESCE(description, ''), 160), ' | ' ORDER BY created_at, id) AS descriptions
FROM money_transaction
WHERE source IS NOT NULL
  AND source_operation_id IS NOT NULL
GROUP BY profile_id, source, source_operation_id
HAVING COUNT(*) > 1
ORDER BY transaction_count DESC, profile_id, source, source_operation_id;

-- 2) Duplicated (profile_id, source_hash) rows that would block the strict unique index.
SELECT
    profile_id,
    source_hash,
    COUNT(*) AS transaction_count,
    ARRAY_AGG(id ORDER BY created_at, id) AS transaction_ids,
    STRING_AGG(LEFT(COALESCE(description, ''), 160), ' | ' ORDER BY created_at, id) AS descriptions
FROM money_transaction
WHERE source_hash IS NOT NULL
GROUP BY profile_id, source_hash
HAVING COUNT(*) > 1
ORDER BY transaction_count DESC, profile_id, source_hash;

-- 3) CJPRESTAMOS-like transactions without an external_sync_mapping row.
SELECT
    tx.profile_id,
    tx.id AS transaction_id,
    tx.real_date,
    tx.amount,
    tx.currency,
    tx.source,
    tx.source_operation_id,
    tx.source_hash,
    tx.description,
    tx.classification_reason
FROM money_transaction tx
WHERE (
        tx.source = 'CJPRESTAMOS'
        OR tx.description ILIKE '%CJ%'
        OR tx.classification_reason ILIKE '%CJ%'
      )
  AND NOT EXISTS (
        SELECT 1
        FROM external_sync_mapping mapping
        WHERE mapping.profile_id = tx.profile_id
          AND mapping.money_transaction_id = tx.id
      )
ORDER BY tx.profile_id, tx.real_date, tx.created_at, tx.id;

-- 4) external_sync_mapping rows pointing to missing money_transaction rows.
SELECT
    mapping.profile_id,
    mapping.id AS mapping_id,
    mapping.external_system,
    mapping.external_entity_type,
    mapping.external_entity_id,
    mapping.external_event_type,
    mapping.money_transaction_id,
    mapping.status,
    mapping.created_at,
    mapping.updated_at
FROM external_sync_mapping mapping
LEFT JOIN money_transaction tx ON tx.id = mapping.money_transaction_id
WHERE mapping.money_transaction_id IS NOT NULL
  AND tx.id IS NULL
ORDER BY mapping.profile_id, mapping.created_at, mapping.id;
