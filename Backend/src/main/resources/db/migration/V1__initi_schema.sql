-- =============================================
-- SQL DDL para PostgreSQL: Gestion de Hogar (mejorado)
-- =============================================

-- 1. Tipos ENUM
CREATE TYPE role_enum AS ENUM (
    'ADMIN',
    'MEMBER',
    'VIEWER'
    );

CREATE TYPE recurring_frequency_enum AS ENUM (
    'DAILY',
    'WEEKLY',
    'BIWEEKLY',
    'MONTHLY',
    'QUARTERLY',
    'SEMIANNUAL',
    'ANNUAL'
    );

CREATE TYPE event_type_enum AS ENUM (
    'LOW_STOCK',
    'RECURRENT_EXPENSE_DUE',
    'ANOMALY_DETECTED',
    'EXPENSE_REMINDER'
    );

CREATE TYPE channel_enum AS ENUM (
    'EMAIL',
    'PUSH',
    'SMS'
    );

CREATE TYPE notification_frequency_enum AS ENUM (
    'INSTANT',
    'DAILY_SUMMARY',
    'WEEKLY_SUMMARY'
    );

-- 2. Funcion y trigger generico para updated_at
CREATE FUNCTION set_updated_at()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.updated_at := NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 3. Tabla de Usuarios
CREATE TABLE users
(
    id            INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username      VARCHAR(50)              NOT NULL UNIQUE,
    email         VARCHAR(100)             NOT NULL UNIQUE,
    password_hash VARCHAR(255)             NOT NULL,
    timezone      VARCHAR(50)              NOT NULL DEFAULT 'America/Argentina/Buenos_Aires',
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    role          VARCHAR(50)              NOT NULL DEFAULT 'ROLE_MEMBER'
);
CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE
    ON users
    FOR EACH ROW
EXECUTE PROCEDURE set_updated_at();

-- 4. Tabla de Casas
CREATE TABLE houses
(
    id         INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre     VARCHAR(100)             NOT NULL,
    direccion  VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE TRIGGER trg_houses_updated_at
    BEFORE UPDATE
    ON houses
    FOR EACH ROW
EXECUTE PROCEDURE set_updated_at();

-- 5. Tabla de Familias
CREATE TABLE families
(
    id          INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    house_id    INTEGER                  NOT NULL
        REFERENCES houses (id)
            ON DELETE CASCADE,
    nombre      VARCHAR(100)             NOT NULL,
    descripcion TEXT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE TRIGGER trg_families_updated_at
    BEFORE UPDATE
    ON families
    FOR EACH ROW
EXECUTE PROCEDURE set_updated_at();

-- 6. Tabla de Membresias de Familia (join table)
CREATE TABLE family_memberships
(
    id        INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id   INTEGER                  NOT NULL
        REFERENCES users (id)
            ON DELETE CASCADE,
    family_id INTEGER                  NOT NULL
        REFERENCES families (id)
            ON DELETE CASCADE,
    role      role_enum                NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, family_id)
);

-- 7. Catalogo de Unidades
CREATE TABLE units
(
    id          INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo      VARCHAR(10)  NOT NULL,
    descripcion VARCHAR(100) NOT NULL,
    is_custom   BOOLEAN      NOT NULL DEFAULT FALSE
);

-- 8. Catalogo de Categorias
CREATE TABLE categories
(
    id          INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    family_id   INTEGER                  NOT NULL
        REFERENCES families (id)
            ON DELETE CASCADE,
    nombre      VARCHAR(100)             NOT NULL,
    descripcion TEXT,
    parent_id   INTEGER
                                         REFERENCES categories (id)
                                             ON DELETE SET NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (family_id, nombre)
);
CREATE TRIGGER trg_categories_updated_at
    BEFORE UPDATE
    ON categories
    FOR EACH ROW
EXECUTE PROCEDURE set_updated_at();

-- 9. Tabla de Inventario
CREATE TABLE inventory_items
(
    id            INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    family_id     INTEGER                  NOT NULL
        REFERENCES families (id)
            ON DELETE CASCADE,
    user_id       INTEGER                  NULL
        REFERENCES users (id)
            ON DELETE SET NULL,
    unit_id       INTEGER                  NOT NULL
        REFERENCES units (id),
    nombre        VARCHAR(150)             NOT NULL,
    quantity      NUMERIC(12, 4)           NOT NULL CHECK (quantity >= 0),
    min_threshold NUMERIC(12, 4)           NOT NULL DEFAULT 0 CHECK (min_threshold >= 0),
    purchase_date DATE,
    expiry_date   DATE,
    barcode       VARCHAR(100),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE TRIGGER trg_inventory_items_updated_at
    BEFORE UPDATE
    ON inventory_items
    FOR EACH ROW
EXECUTE PROCEDURE set_updated_at();

-- 10. Tabla de Gastos
CREATE TABLE expenses
(
    id             INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    family_id      INTEGER                  NOT NULL
        REFERENCES families (id)
            ON DELETE CASCADE,
    user_id        INTEGER                  NULL
        REFERENCES users (id)
            ON DELETE SET NULL,
    amount         NUMERIC(14, 2)           NOT NULL CHECK (amount >= 0),
    currency       CHAR(3)                  NOT NULL DEFAULT 'ARS',
    category_id    INTEGER
                                            REFERENCES categories (id)
                                                ON DELETE SET NULL,
    subcategory_id INTEGER
                                            REFERENCES categories (id)
                                                ON DELETE SET NULL,
    descripcion    TEXT,
    date           DATE                     NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE TRIGGER trg_expenses_updated_at
    BEFORE UPDATE
    ON expenses
    FOR EACH ROW
EXECUTE PROCEDURE set_updated_at();

-- 11. Tabla de Gastos Recurrentes
CREATE TABLE recurring_expenses
(
    id                  INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    expense_template_id INTEGER                  NOT NULL
        REFERENCES expenses (id)
            ON DELETE CASCADE,
    frequency           recurring_frequency_enum NOT NULL,
    next_due_date       DATE                     NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE TRIGGER trg_recurring_expenses_updated_at
    BEFORE UPDATE
    ON recurring_expenses
    FOR EACH ROW
EXECUTE PROCEDURE set_updated_at();

-- 12. Configuracion de Notificaciones
CREATE TABLE notification_settings
(
    id          INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     INTEGER                       NOT NULL
        REFERENCES users (id)
            ON DELETE CASCADE,
    event_type  event_type_enum               NOT NULL,
    channels    channel_enum[]                NOT NULL,
    frequencies notification_frequency_enum[] NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE      NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE      NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, event_type)
);
CREATE TRIGGER trg_notification_settings_updated_at
    BEFORE UPDATE
    ON notification_settings
    FOR EACH ROW
EXECUTE PROCEDURE set_updated_at();

-- 13. indices Adicionales
CREATE INDEX idx_inventory_family ON inventory_items (family_id);
CREATE INDEX idx_expense_family ON expenses (family_id);
CREATE INDEX idx_recurring_expense_next_due ON recurring_expenses (next_due_date);
CREATE INDEX idx_category_family ON categories (family_id);
CREATE INDEX idx_notification_user ON notification_settings (user_id);

-- =============================================
-- Fin de DDL
-- =============================================
