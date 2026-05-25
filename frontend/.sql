-- ============================================================
-- MODULO: Compras - Requerimientos
-- Etapa 1: modulo funcional minimo de REQUERIMIENTOS
-- PostgreSQL 9.6+
--
-- Tablas finales:
--   compras.requerimientos
--   compras.requerimientos_detalle
--   compras.requerimientos_sectores
--   compras.requerimientos_estados
-- ============================================================
BEGIN;
-- ============================================================
-- 2) SCHEMA
-- ============================================================
CREATE SCHEMA compras;
-- ============================================================
-- 3) SECUENCIA DE NUMERO VISIBLE
-- ============================================================
CREATE SEQUENCE compras.requerimientos_numero_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
-- ============================================================
-- 4) ESTADOS DEL REQUERIMIENTO
-- ============================================================
CREATE TABLE compras.requerimientos_estados (
    id_estado INTEGER PRIMARY KEY,
    codigo VARCHAR(40) NOT NULL UNIQUE,
    descripcion VARCHAR(120) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    es_final BOOLEAN NOT NULL DEFAULT FALSE,
    orden INTEGER NOT NULL
);
-- ============================================================
-- 5) SECTORES SOLICITANTES
-- ============================================================
CREATE TABLE compras.requerimientos_sectores (
    id_sector SERIAL PRIMARY KEY,
    codigo VARCHAR(40) NOT NULL UNIQUE,
    descripcion VARCHAR(120) NOT NULL,
    -- Define si normalmente este sector requiere buscar afiliado.
    requiere_afiliado BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    alta_fecha TIMESTAMP NOT NULL DEFAULT now(),
    alta_usr VARCHAR(75) NOT NULL DEFAULT 'sistema',
    modi_fecha TIMESTAMP NULL,
    modi_usr VARCHAR(75) NULL,
    baja_fecha TIMESTAMP NULL,
    baja_usr VARCHAR(75) NULL
);
-- ============================================================
-- 6) CABECERA DEL REQUERIMIENTO
-- ============================================================
CREATE TABLE compras.requerimientos (
    id_requerimiento SERIAL PRIMARY KEY,
    -- Numero visible del requerimiento.
    -- Es distinto del id tecnico.
    numero INTEGER NOT NULL DEFAULT nextval('compras.requerimientos_numero_seq'::regclass),
    id_estado INTEGER NOT NULL DEFAULT 1 REFERENCES compras.requerimientos_estados(id_estado),
    id_sector INTEGER NOT NULL REFERENCES compras.requerimientos_sectores(id_sector),
    -- Snapshot de la regla del sector al momento de cargar.
    requiere_afiliado BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_solicitud DATE NOT NULL DEFAULT CURRENT_DATE,
    solicitante_usr VARCHAR(75) NOT NULL,
    solicitante_nombre VARCHAR(150) NULL,
    -- Referencia al afiliado legacy.
    -- El sistema actual trabaja con cuil_titular + inte.
    afiliado_cuil_titular VARCHAR(20) NULL,
    afiliado_inte INTEGER NULL,
    -- Datos principales del pedido.
    descripcion TEXT NOT NULL,
    observaciones TEXT NULL,
    -- Auditoria tecnica.
    alta_fecha TIMESTAMP NOT NULL DEFAULT now(),
    alta_usr VARCHAR(75) NOT NULL,
    modi_fecha TIMESTAMP NULL,
    modi_usr VARCHAR(75) NULL,
    baja_fecha TIMESTAMP NULL,
    baja_usr VARCHAR(75) NULL,
    CONSTRAINT uq_compras_requerimientos_numero UNIQUE (numero),
    CONSTRAINT chk_compras_req_descripcion_no_vacia CHECK (length(trim(descripcion)) > 0),
    CONSTRAINT chk_compras_req_afiliado_inte_valido CHECK (
        afiliado_inte IS NULL
        OR afiliado_inte >= 0
    )
);
-- ============================================================
-- 7) DETALLE DEL REQUERIMIENTO
-- ============================================================
CREATE TABLE compras.requerimientos_detalle (
    id_requerimiento_detalle SERIAL PRIMARY KEY,
    id_requerimiento INTEGER NOT NULL REFERENCES compras.requerimientos(id_requerimiento),
    renglon INTEGER NOT NULL,
    -- Texto libre por ahora. No catalogo.
    -- Ej: MEDICACION, LECHE, PANIALES, PROTESIS, INSUMO, OTRO.
    tipo_articulo VARCHAR(80) NULL,
    articulo TEXT NOT NULL,
    cantidad NUMERIC(14, 2) NOT NULL DEFAULT 1,
    unidad_medida VARCHAR(30) NULL,
    precio_unitario_estimado NUMERIC(14, 2) NULL,
    precio_total_estimado NUMERIC(14, 2) NULL,
    observaciones TEXT NULL,
    alta_fecha TIMESTAMP NOT NULL DEFAULT now(),
    alta_usr VARCHAR(75) NOT NULL,
    modi_fecha TIMESTAMP NULL,
    modi_usr VARCHAR(75) NULL,
    baja_fecha TIMESTAMP NULL,
    baja_usr VARCHAR(75) NULL,
    CONSTRAINT uq_compras_req_detalle_renglon UNIQUE (id_requerimiento, renglon),
    CONSTRAINT chk_compras_req_detalle_renglon CHECK (renglon > 0),
    CONSTRAINT chk_compras_req_detalle_articulo_no_vacio CHECK (length(trim(articulo)) > 0),
    CONSTRAINT chk_compras_req_detalle_cantidad CHECK (cantidad > 0),
    CONSTRAINT chk_compras_req_detalle_precio_unitario CHECK (
        precio_unitario_estimado IS NULL
        OR precio_unitario_estimado >= 0
    ),
    CONSTRAINT chk_compras_req_detalle_precio_total CHECK (
        precio_total_estimado IS NULL
        OR precio_total_estimado >= 0
    )
);
-- ============================================================
-- 8) INDICES
-- ============================================================
CREATE INDEX idx_compras_req_numero ON compras.requerimientos(numero);
CREATE INDEX idx_compras_req_estado ON compras.requerimientos(id_estado);
CREATE INDEX idx_compras_req_sector ON compras.requerimientos(id_sector);
CREATE INDEX idx_compras_req_fecha_solicitud ON compras.requerimientos(fecha_solicitud);
CREATE INDEX idx_compras_req_solicitante_usr ON compras.requerimientos(solicitante_usr);
CREATE INDEX idx_compras_req_afiliado_clave ON compras.requerimientos(afiliado_cuil_titular, afiliado_inte);
CREATE INDEX idx_compras_req_baja ON compras.requerimientos(baja_fecha);
CREATE INDEX idx_compras_req_detalle_req ON compras.requerimientos_detalle(id_requerimiento);
CREATE INDEX idx_compras_req_detalle_tipo_articulo ON compras.requerimientos_detalle(tipo_articulo);
CREATE INDEX idx_compras_req_detalle_baja ON compras.requerimientos_detalle(baja_fecha);
-- ============================================================
-- 9) DATOS INICIALES
-- ============================================================
INSERT INTO compras.requerimientos_estados (
        id_estado,
        codigo,
        descripcion,
        activo,
        es_final,
        orden
    )
VALUES (1, 'BORRADOR', 'Borrador', TRUE, FALSE, 1),
    (2, 'SOLICITADO', 'Solicitado', TRUE, FALSE, 2),
    (9, 'ANULADO', 'Anulado', TRUE, TRUE, 99);
INSERT INTO compras.requerimientos_sectores (
        codigo,
        descripcion,
        requiere_afiliado,
        activo,
        alta_usr
    )
VALUES ('FARMACIA', 'Farmacia', TRUE, TRUE, 'sistema'),
    (
        'PRESTACIONES_MEDICAS',
        'Prestaciones Medicas',
        TRUE,
        TRUE,
        'sistema'
    ),
    (
        'AUDITORIA_MEDICA',
        'Auditoria Medica',
        TRUE,
        TRUE,
        'sistema'
    ),
    (
        'MONOTRIBUTO',
        'Monotributo',
        TRUE,
        TRUE,
        'sistema'
    ),
    ('SISTEMAS', 'Sistemas', FALSE, TRUE, 'sistema'),
    ('RRHH', 'RRHH', FALSE, TRUE, 'sistema'),
    ('LEGALES', 'Legales', FALSE, TRUE, 'sistema'),
    ('OTROS', 'Otros', FALSE, TRUE, 'sistema');
-- ============================================================
-- 10) FUNCIONES SQL DEL SUBMODULO REQUERIMIENTOS
-- ============================================================
-- ============================================================
-- BUSCAR REQUERIMIENTOS
-- ============================================================
CREATE OR REPLACE FUNCTION compras.buscar_requerimientos(
        p_numero INTEGER,
        p_fecha_desde DATE,
        p_fecha_hasta DATE,
        p_id_sector INTEGER,
        p_id_estado INTEGER,
        p_solicitante_usr VARCHAR,
        p_afiliado_cuil_titular VARCHAR,
        p_afiliado_inte INTEGER,
        p_tipo_articulo VARCHAR,
        p_texto VARCHAR
    ) RETURNS TABLE (
        id_requerimiento INTEGER,
        numero INTEGER,
        id_estado INTEGER,
        estado_codigo VARCHAR,
        estado_descripcion VARCHAR,
        id_sector INTEGER,
        sector_codigo VARCHAR,
        sector_descripcion VARCHAR,
        requiere_afiliado BOOLEAN,
        fecha_solicitud DATE,
        solicitante_usr VARCHAR,
        solicitante_nombre VARCHAR,
        afiliado_cuil_titular VARCHAR,
        afiliado_inte INTEGER,
        descripcion TEXT,
        observaciones TEXT,
        alta_fecha TIMESTAMP,
        alta_usr VARCHAR,
        modi_fecha TIMESTAMP,
        modi_usr VARCHAR,
        baja_fecha TIMESTAMP,
        baja_usr VARCHAR
    ) AS $$ BEGIN RETURN QUERY
SELECT r.id_requerimiento,
    r.numero,
    r.id_estado,
    e.codigo AS estado_codigo,
    e.descripcion AS estado_descripcion,
    r.id_sector,
    s.codigo AS sector_codigo,
    s.descripcion AS sector_descripcion,
    r.requiere_afiliado,
    r.fecha_solicitud,
    r.solicitante_usr,
    r.solicitante_nombre,
    r.afiliado_cuil_titular,
    r.afiliado_inte,
    r.descripcion,
    r.observaciones,
    r.alta_fecha,
    r.alta_usr,
    r.modi_fecha,
    r.modi_usr,
    r.baja_fecha,
    r.baja_usr
FROM compras.requerimientos r
    JOIN compras.requerimientos_estados e ON e.id_estado = r.id_estado
    JOIN compras.requerimientos_sectores s ON s.id_sector = r.id_sector
WHERE r.baja_fecha IS NULL
    AND (
        p_numero IS NULL
        OR r.numero = p_numero
    )
    AND (
        p_fecha_desde IS NULL
        OR r.fecha_solicitud >= p_fecha_desde
    )
    AND (
        p_fecha_hasta IS NULL
        OR r.fecha_solicitud <= p_fecha_hasta
    )
    AND (
        p_id_sector IS NULL
        OR r.id_sector = p_id_sector
    )
    AND (
        p_id_estado IS NULL
        OR r.id_estado = p_id_estado
    )
    AND (
        p_solicitante_usr IS NULL
        OR lower(r.solicitante_usr) LIKE '%' || lower(trim(p_solicitante_usr)) || '%'
    )
    AND (
        p_afiliado_cuil_titular IS NULL
        OR r.afiliado_cuil_titular LIKE '%' || trim(p_afiliado_cuil_titular) || '%'
    )
    AND (
        p_afiliado_inte IS NULL
        OR r.afiliado_inte = p_afiliado_inte
    )
    AND (
        p_tipo_articulo IS NULL
        OR EXISTS (
            SELECT 1
            FROM compras.requerimientos_detalle d
            WHERE d.id_requerimiento = r.id_requerimiento
                AND d.baja_fecha IS NULL
                AND lower(coalesce(d.tipo_articulo, '')) LIKE '%' || lower(trim(p_tipo_articulo)) || '%'
        )
    )
    AND (
        p_texto IS NULL
        OR lower(coalesce(r.descripcion, '')) LIKE '%' || lower(trim(p_texto)) || '%'
        OR lower(coalesce(r.observaciones, '')) LIKE '%' || lower(trim(p_texto)) || '%'
        OR lower(coalesce(r.solicitante_usr, '')) LIKE '%' || lower(trim(p_texto)) || '%'
        OR lower(coalesce(r.solicitante_nombre, '')) LIKE '%' || lower(trim(p_texto)) || '%'
        OR coalesce(r.afiliado_cuil_titular, '') LIKE '%' || trim(p_texto) || '%'
        OR EXISTS (
            SELECT 1
            FROM compras.requerimientos_detalle d2
            WHERE d2.id_requerimiento = r.id_requerimiento
                AND d2.baja_fecha IS NULL
                AND (
                    lower(coalesce(d2.articulo, '')) LIKE '%' || lower(trim(p_texto)) || '%'
                    OR lower(coalesce(d2.tipo_articulo, '')) LIKE '%' || lower(trim(p_texto)) || '%'
                    OR lower(coalesce(d2.observaciones, '')) LIKE '%' || lower(trim(p_texto)) || '%'
                )
        )
    )
ORDER BY r.fecha_solicitud DESC,
    r.numero DESC;
END;
$$ LANGUAGE plpgsql;
-- ============================================================
-- OBTENER CABECERA
-- ============================================================
CREATE OR REPLACE FUNCTION compras.get_requerimiento(p_id_requerimiento INTEGER) RETURNS TABLE (
        id_requerimiento INTEGER,
        numero INTEGER,
        id_estado INTEGER,
        estado_codigo VARCHAR,
        estado_descripcion VARCHAR,
        id_sector INTEGER,
        sector_codigo VARCHAR,
        sector_descripcion VARCHAR,
        requiere_afiliado BOOLEAN,
        fecha_solicitud DATE,
        solicitante_usr VARCHAR,
        solicitante_nombre VARCHAR,
        afiliado_cuil_titular VARCHAR,
        afiliado_inte INTEGER,
        descripcion TEXT,
        observaciones TEXT,
        alta_fecha TIMESTAMP,
        alta_usr VARCHAR,
        modi_fecha TIMESTAMP,
        modi_usr VARCHAR,
        baja_fecha TIMESTAMP,
        baja_usr VARCHAR
    ) AS $$ BEGIN RETURN QUERY
SELECT r.id_requerimiento,
    r.numero,
    r.id_estado,
    e.codigo AS estado_codigo,
    e.descripcion AS estado_descripcion,
    r.id_sector,
    s.codigo AS sector_codigo,
    s.descripcion AS sector_descripcion,
    r.requiere_afiliado,
    r.fecha_solicitud,
    r.solicitante_usr,
    r.solicitante_nombre,
    r.afiliado_cuil_titular,
    r.afiliado_inte,
    r.descripcion,
    r.observaciones,
    r.alta_fecha,
    r.alta_usr,
    r.modi_fecha,
    r.modi_usr,
    r.baja_fecha,
    r.baja_usr
FROM compras.requerimientos r
    JOIN compras.requerimientos_estados e ON e.id_estado = r.id_estado
    JOIN compras.requerimientos_sectores s ON s.id_sector = r.id_sector
WHERE r.id_requerimiento = p_id_requerimiento;
END;
$$ LANGUAGE plpgsql;
-- ============================================================
-- OBTENER DETALLE
-- ============================================================
CREATE OR REPLACE FUNCTION compras.get_requerimiento_detalle(p_id_requerimiento INTEGER) RETURNS TABLE (
        id_requerimiento_detalle INTEGER,
        id_requerimiento INTEGER,
        renglon INTEGER,
        tipo_articulo VARCHAR,
        articulo TEXT,
        cantidad NUMERIC,
        unidad_medida VARCHAR,
        precio_unitario_estimado NUMERIC,
        precio_total_estimado NUMERIC,
        observaciones TEXT,
        alta_fecha TIMESTAMP,
        alta_usr VARCHAR,
        modi_fecha TIMESTAMP,
        modi_usr VARCHAR,
        baja_fecha TIMESTAMP,
        baja_usr VARCHAR
    ) AS $$ BEGIN RETURN QUERY
SELECT d.id_requerimiento_detalle,
    d.id_requerimiento,
    d.renglon,
    d.tipo_articulo,
    d.articulo,
    d.cantidad,
    d.unidad_medida,
    d.precio_unitario_estimado,
    d.precio_total_estimado,
    d.observaciones,
    d.alta_fecha,
    d.alta_usr,
    d.modi_fecha,
    d.modi_usr,
    d.baja_fecha,
    d.baja_usr
FROM compras.requerimientos_detalle d
WHERE d.id_requerimiento = p_id_requerimiento
    AND d.baja_fecha IS NULL
ORDER BY d.renglon ASC,
    d.id_requerimiento_detalle ASC;
END;
$$ LANGUAGE plpgsql;
-- ============================================================
-- GUARDAR CABECERA
-- ============================================================
CREATE OR REPLACE FUNCTION compras.guardar_requerimiento(
        p_id_requerimiento INTEGER,
        p_numero INTEGER,
        p_id_estado INTEGER,
        p_id_sector INTEGER,
        p_requiere_afiliado BOOLEAN,
        p_fecha_solicitud DATE,
        p_solicitante_usr VARCHAR,
        p_solicitante_nombre VARCHAR,
        p_afiliado_cuil_titular VARCHAR,
        p_afiliado_inte INTEGER,
        p_descripcion TEXT,
        p_observaciones TEXT,
        p_usuario VARCHAR
    ) RETURNS INTEGER AS $$
DECLARE v_id_requerimiento INTEGER;
v_requiere_afiliado BOOLEAN;
BEGIN IF p_id_sector IS NULL
OR p_id_sector <= 0 THEN RAISE EXCEPTION 'Debe informar el sector solicitante.';
END IF;
IF p_solicitante_usr IS NULL
OR length(trim(p_solicitante_usr)) = 0 THEN RAISE EXCEPTION 'Debe informar el solicitante.';
END IF;
IF p_descripcion IS NULL
OR length(trim(p_descripcion)) = 0 THEN RAISE EXCEPTION 'Debe informar la descripcion del requerimiento.';
END IF;
SELECT COALESCE(p_requiere_afiliado, FALSE)
    OR COALESCE(s.requiere_afiliado, FALSE) INTO v_requiere_afiliado
FROM compras.requerimientos_sectores s
WHERE s.id_sector = p_id_sector
    AND s.activo = TRUE
    AND s.baja_fecha IS NULL;
IF v_requiere_afiliado IS NULL THEN RAISE EXCEPTION 'El sector informado no existe o no esta activo.';
END IF;
IF v_requiere_afiliado = TRUE THEN IF p_afiliado_cuil_titular IS NULL
OR length(trim(p_afiliado_cuil_titular)) = 0 THEN RAISE EXCEPTION 'Debe informar el CUIL titular del afiliado.';
END IF;
IF p_afiliado_inte IS NULL
OR p_afiliado_inte < 0 THEN RAISE EXCEPTION 'Debe informar el numero de integrante del afiliado.';
END IF;
END IF;
IF p_id_requerimiento IS NULL
OR p_id_requerimiento <= 0 THEN IF p_numero IS NULL
OR p_numero <= 0 THEN
INSERT INTO compras.requerimientos (
        id_estado,
        id_sector,
        requiere_afiliado,
        fecha_solicitud,
        solicitante_usr,
        solicitante_nombre,
        afiliado_cuil_titular,
        afiliado_inte,
        descripcion,
        observaciones,
        alta_usr
    )
VALUES (
        COALESCE(p_id_estado, 1),
        p_id_sector,
        v_requiere_afiliado,
        COALESCE(p_fecha_solicitud, CURRENT_DATE),
        trim(p_solicitante_usr),
        NULLIF(trim(COALESCE(p_solicitante_nombre, '')), ''),
        NULLIF(trim(COALESCE(p_afiliado_cuil_titular, '')), ''),
        p_afiliado_inte,
        trim(p_descripcion),
        NULLIF(trim(COALESCE(p_observaciones, '')), ''),
        trim(p_usuario)
    )
RETURNING id_requerimiento INTO v_id_requerimiento;
ELSE
INSERT INTO compras.requerimientos (
        numero,
        id_estado,
        id_sector,
        requiere_afiliado,
        fecha_solicitud,
        solicitante_usr,
        solicitante_nombre,
        afiliado_cuil_titular,
        afiliado_inte,
        descripcion,
        observaciones,
        alta_usr
    )
VALUES (
        p_numero,
        COALESCE(p_id_estado, 1),
        p_id_sector,
        v_requiere_afiliado,
        COALESCE(p_fecha_solicitud, CURRENT_DATE),
        trim(p_solicitante_usr),
        NULLIF(trim(COALESCE(p_solicitante_nombre, '')), ''),
        NULLIF(trim(COALESCE(p_afiliado_cuil_titular, '')), ''),
        p_afiliado_inte,
        trim(p_descripcion),
        NULLIF(trim(COALESCE(p_observaciones, '')), ''),
        trim(p_usuario)
    )
RETURNING id_requerimiento INTO v_id_requerimiento;
END IF;
RETURN v_id_requerimiento;
END IF;
UPDATE compras.requerimientos
SET numero = COALESCE(NULLIF(p_numero, 0), numero),
    id_estado = COALESCE(p_id_estado, id_estado),
    id_sector = p_id_sector,
    requiere_afiliado = v_requiere_afiliado,
    fecha_solicitud = COALESCE(p_fecha_solicitud, fecha_solicitud),
    solicitante_usr = trim(p_solicitante_usr),
    solicitante_nombre = NULLIF(trim(COALESCE(p_solicitante_nombre, '')), ''),
    afiliado_cuil_titular = NULLIF(trim(COALESCE(p_afiliado_cuil_titular, '')), ''),
    afiliado_inte = p_afiliado_inte,
    descripcion = trim(p_descripcion),
    observaciones = NULLIF(trim(COALESCE(p_observaciones, '')), ''),
    modi_fecha = now(),
    modi_usr = trim(p_usuario)
WHERE id_requerimiento = p_id_requerimiento
    AND baja_fecha IS NULL;
IF NOT FOUND THEN RAISE EXCEPTION 'No se encontro el requerimiento a modificar.';
END IF;
RETURN p_id_requerimiento;
END;
$$ LANGUAGE plpgsql;
-- ============================================================
-- GUARDAR DETALLE
-- ============================================================
CREATE OR REPLACE FUNCTION compras.guardar_requerimiento_detalle(
        p_id_requerimiento_detalle INTEGER,
        p_id_requerimiento INTEGER,
        p_renglon INTEGER,
        p_tipo_articulo VARCHAR,
        p_articulo TEXT,
        p_cantidad NUMERIC,
        p_unidad_medida VARCHAR,
        p_precio_unitario_estimado NUMERIC,
        p_precio_total_estimado NUMERIC,
        p_observaciones TEXT,
        p_usuario VARCHAR
    ) RETURNS INTEGER AS $$
DECLARE v_id_requerimiento_detalle INTEGER;
v_renglon INTEGER;
v_cantidad NUMERIC;
v_precio_total NUMERIC;
BEGIN IF p_id_requerimiento IS NULL
OR p_id_requerimiento <= 0 THEN RAISE EXCEPTION 'Debe informar el requerimiento.';
END IF;
IF NOT EXISTS (
    SELECT 1
    FROM compras.requerimientos r
    WHERE r.id_requerimiento = p_id_requerimiento
        AND r.baja_fecha IS NULL
) THEN RAISE EXCEPTION 'No se encontro el requerimiento informado.';
END IF;
IF p_articulo IS NULL
OR length(trim(p_articulo)) = 0 THEN RAISE EXCEPTION 'Debe informar el articulo.';
END IF;
v_cantidad := COALESCE(p_cantidad, 1);
IF v_cantidad <= 0 THEN RAISE EXCEPTION 'La cantidad debe ser mayor a cero.';
END IF;
IF p_precio_unitario_estimado IS NOT NULL
AND p_precio_unitario_estimado < 0 THEN RAISE EXCEPTION 'El precio unitario estimado no puede ser negativo.';
END IF;
IF p_precio_total_estimado IS NOT NULL
AND p_precio_total_estimado < 0 THEN RAISE EXCEPTION 'El precio total estimado no puede ser negativo.';
END IF;
v_precio_total := p_precio_total_estimado;
IF v_precio_total IS NULL
AND p_precio_unitario_estimado IS NOT NULL THEN v_precio_total := v_cantidad * p_precio_unitario_estimado;
END IF;
IF p_id_requerimiento_detalle IS NULL
OR p_id_requerimiento_detalle <= 0 THEN IF p_renglon IS NULL
OR p_renglon <= 0 THEN
SELECT COALESCE(MAX(d.renglon), 0) + 1 INTO v_renglon
FROM compras.requerimientos_detalle d
WHERE d.id_requerimiento = p_id_requerimiento;
ELSE v_renglon := p_renglon;
END IF;
INSERT INTO compras.requerimientos_detalle (
        id_requerimiento,
        renglon,
        tipo_articulo,
        articulo,
        cantidad,
        unidad_medida,
        precio_unitario_estimado,
        precio_total_estimado,
        observaciones,
        alta_usr
    )
VALUES (
        p_id_requerimiento,
        v_renglon,
        NULLIF(trim(COALESCE(p_tipo_articulo, '')), ''),
        trim(p_articulo),
        v_cantidad,
        NULLIF(trim(COALESCE(p_unidad_medida, '')), ''),
        p_precio_unitario_estimado,
        v_precio_total,
        NULLIF(trim(COALESCE(p_observaciones, '')), ''),
        trim(p_usuario)
    )
RETURNING id_requerimiento_detalle INTO v_id_requerimiento_detalle;
RETURN v_id_requerimiento_detalle;
END IF;
IF p_renglon IS NULL
OR p_renglon <= 0 THEN
SELECT d.renglon INTO v_renglon
FROM compras.requerimientos_detalle d
WHERE d.id_requerimiento_detalle = p_id_requerimiento_detalle;
ELSE v_renglon := p_renglon;
END IF;
UPDATE compras.requerimientos_detalle
SET renglon = v_renglon,
    tipo_articulo = NULLIF(trim(COALESCE(p_tipo_articulo, '')), ''),
    articulo = trim(p_articulo),
    cantidad = v_cantidad,
    unidad_medida = NULLIF(trim(COALESCE(p_unidad_medida, '')), ''),
    precio_unitario_estimado = p_precio_unitario_estimado,
    precio_total_estimado = v_precio_total,
    observaciones = NULLIF(trim(COALESCE(p_observaciones, '')), ''),
    modi_fecha = now(),
    modi_usr = trim(p_usuario)
WHERE id_requerimiento_detalle = p_id_requerimiento_detalle
    AND baja_fecha IS NULL;
IF NOT FOUND THEN RAISE EXCEPTION 'No se encontro el renglon a modificar.';
END IF;
RETURN p_id_requerimiento_detalle;
END;
$$ LANGUAGE plpgsql;
-- ============================================================
-- BORRAR CABECERA
-- ============================================================
CREATE OR REPLACE FUNCTION compras.borrar_requerimiento(
        p_id_requerimiento INTEGER,
        p_usuario VARCHAR
    ) RETURNS VOID AS $$ BEGIN
UPDATE compras.requerimientos
SET id_estado = 9,
    baja_fecha = now(),
    baja_usr = trim(p_usuario),
    modi_fecha = now(),
    modi_usr = trim(p_usuario)
WHERE id_requerimiento = p_id_requerimiento
    AND baja_fecha IS NULL;
UPDATE compras.requerimientos_detalle
SET baja_fecha = now(),
    baja_usr = trim(p_usuario),
    modi_fecha = now(),
    modi_usr = trim(p_usuario)
WHERE id_requerimiento = p_id_requerimiento
    AND baja_fecha IS NULL;
END;
$$ LANGUAGE plpgsql;
-- ============================================================
-- BORRAR DETALLE
-- ============================================================
CREATE OR REPLACE FUNCTION compras.borrar_requerimiento_detalle(
        p_id_requerimiento_detalle INTEGER,
        p_usuario VARCHAR
    ) RETURNS VOID AS $$ BEGIN
UPDATE compras.requerimientos_detalle
SET baja_fecha = now(),
    baja_usr = trim(p_usuario),
    modi_fecha = now(),
    modi_usr = trim(p_usuario)
WHERE id_requerimiento_detalle = p_id_requerimiento_detalle
    AND baja_fecha IS NULL;
END;
$$ LANGUAGE plpgsql;
-- ============================================================
-- CAMBIAR ESTADO
-- ============================================================
CREATE OR REPLACE FUNCTION compras.cambiar_estado_requerimiento(
        p_id_requerimiento INTEGER,
        p_id_estado INTEGER,
        p_usuario VARCHAR
    ) RETURNS VOID AS $$ BEGIN IF NOT EXISTS (
        SELECT 1
        FROM compras.requerimientos_estados e
        WHERE e.id_estado = p_id_estado
            AND e.activo = TRUE
    ) THEN RAISE EXCEPTION 'Estado de requerimiento invalido.';
END IF;
UPDATE compras.requerimientos
SET id_estado = p_id_estado,
    modi_fecha = now(),
    modi_usr = trim(p_usuario)
WHERE id_requerimiento = p_id_requerimiento
    AND baja_fecha IS NULL;
IF NOT FOUND THEN RAISE EXCEPTION 'No se encontro el requerimiento.';
END IF;
END;
$$ LANGUAGE plpgsql;
COMMIT;