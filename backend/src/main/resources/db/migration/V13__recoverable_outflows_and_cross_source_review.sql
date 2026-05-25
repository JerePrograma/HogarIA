UPDATE category
SET category_key = 'cj_capital_prestado',
    default_movement_type = 'ADJUSTMENT',
    budgetable = FALSE,
    technical = FALSE,
    updated_at = NOW()
WHERE lower(name) = lower('CJ - Capital prestado');

INSERT INTO category (
    id,
    profile_id,
    parent_id,
    name,
    category_key,
    type,
    scope,
    default_movement_type,
    budgetable,
    technical,
    active,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    NULL,
    NULL,
    'CJ - Capital prestado',
    'cj_capital_prestado',
    'INVESTMENT',
    'GLOBAL',
    'ADJUSTMENT',
    FALSE,
    FALSE,
    TRUE,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM category
    WHERE profile_id IS NULL
      AND category_key = 'cj_capital_prestado'
      AND type = 'INVESTMENT'
);

UPDATE money_transaction
SET movement_type = 'ADJUSTMENT',
    updated_at = NOW()
WHERE upper(source) = 'CJPRESTAMOS'
  AND classification_reason = 'CJPRESTAMOS_DISBURSEMENT'
  AND movement_type = 'EXPENSE';
