ALTER TABLE category
    ADD COLUMN IF NOT EXISTS category_key VARCHAR(120);

ALTER TABLE category
    ADD COLUMN IF NOT EXISTS default_movement_type VARCHAR(40);

ALTER TABLE category
    ADD COLUMN IF NOT EXISTS budgetable BOOLEAN DEFAULT TRUE;

ALTER TABLE category
    ADD COLUMN IF NOT EXISTS technical BOOLEAN DEFAULT FALSE;

UPDATE category
SET budgetable = TRUE
WHERE budgetable IS NULL;

UPDATE category
SET technical = FALSE
WHERE technical IS NULL;

ALTER TABLE category
    ALTER COLUMN budgetable SET DEFAULT TRUE;

ALTER TABLE category
    ALTER COLUMN technical SET DEFAULT FALSE;

ALTER TABLE category
    ALTER COLUMN budgetable SET NOT NULL;

ALTER TABLE category
    ALTER COLUMN technical SET NOT NULL;

UPDATE category
SET category_key =
        lower(
                trim(
                        both '_' from regexp_replace(
                        translate(
                                name,
                                'ÁÀÄÂÃáàäâãÉÈËÊéèëêÍÌÏÎíìïîÓÒÖÔÕóòöôõÚÙÜÛúùüûÑñÇç',
                                'AAAAAaaaaaEEEEeeeeIIIIiiiiOOOOOoooooUUUUuuuuNnCc'
                        ),
                        '[^a-zA-Z0-9]+',
                        '_',
                        'g'
                                      )
                )
        )
WHERE category_key IS NULL
   OR btrim(category_key) = '';

UPDATE category
SET default_movement_type =
        CASE
            WHEN type = 'INCOME' THEN 'INCOME'
            WHEN type IN ('FIXED_EXPENSE', 'VARIABLE_EXPENSE') THEN 'EXPENSE'
            WHEN type IN ('SAVING', 'INVESTMENT') THEN 'SAVING'
            WHEN type = 'DEBT' THEN 'ADJUSTMENT'
            ELSE default_movement_type
            END
WHERE default_movement_type IS NULL
   OR btrim(default_movement_type) = '';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_category_default_movement_type'
    ) THEN
ALTER TABLE category
    ADD CONSTRAINT ck_category_default_movement_type
        CHECK (
            default_movement_type IS NULL
                OR default_movement_type IN ('INCOME', 'EXPENSE', 'SAVING', 'TRANSFER', 'ADJUSTMENT')
            );
END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_category_profile_budgetable_technical
    ON category(profile_id, budgetable, technical);

CREATE INDEX IF NOT EXISTS idx_category_global_category_key
    ON category(category_key)
    WHERE profile_id IS NULL;

WITH seed(parent_key, category_key, name, type, scope, default_movement_type, budgetable, technical) AS (
    VALUES
        -- Raíces: ingresos
        (NULL, 'ingresos_laborales', 'Ingresos laborales', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        (NULL, 'ingresos_profesionales', 'Ingresos profesionales', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        (NULL, 'ingresos_negocio', 'Ingresos de negocio', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        (NULL, 'ingresos_financieros', 'Ingresos financieros', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        (NULL, 'ingresos_eventuales', 'Ingresos eventuales', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        (NULL, 'cj_prestamos_ingresos', 'CJ - Ingresos de préstamos', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),

        -- Raíces: gastos fijos
        (NULL, 'vivienda', 'Vivienda', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'servicios', 'Servicios', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'salud_fija', 'Salud fija', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'educacion_fija', 'Educación fija', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'seguros', 'Seguros', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'suscripciones_fijas', 'Suscripciones fijas', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'impuestos_fijos', 'Impuestos fijos', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),

        -- Raíces: gastos variables
        (NULL, 'alimentacion', 'Alimentación', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'transporte', 'Transporte', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'hogar', 'Hogar', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'salud_variable', 'Salud variable', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'familia_e_hijos', 'Familia e hijos', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'mascotas', 'Mascotas', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'indumentaria', 'Indumentaria', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'ocio', 'Ocio', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'viajes', 'Viajes', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'bancos_y_comisiones', 'Bancos y comisiones', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'impuestos_variables', 'Impuestos variables', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        (NULL, 'gastos_generales', 'Gastos generales', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),

        -- Raíces: ahorro, inversión, deuda
        (NULL, 'ahorro', 'Ahorro', 'SAVING', 'GLOBAL', 'SAVING', TRUE, FALSE),
        (NULL, 'transferencias_internas', 'Transferencias internas', 'SAVING', 'GLOBAL', 'TRANSFER', FALSE, TRUE),
        (NULL, 'inversiones', 'Inversiones', 'INVESTMENT', 'GLOBAL', 'SAVING', TRUE, FALSE),
        (NULL, 'cj_prestamos_inversion', 'CJ - Préstamos otorgados', 'INVESTMENT', 'GLOBAL', 'SAVING', TRUE, FALSE),
        (NULL, 'deudas', 'Deudas', 'DEBT', 'GLOBAL', 'ADJUSTMENT', TRUE, FALSE),

        -- Raíz técnica
        (NULL, 'operaciones_tecnicas', 'Operaciones técnicas', 'VARIABLE_EXPENSE', 'GLOBAL', 'ADJUSTMENT', FALSE, TRUE)
),
     roots AS (
         SELECT *
         FROM seed
         WHERE parent_key IS NULL
     )
INSERT INTO category (
    profile_id,
    parent_id,
    name,
    type,
    scope,
    active,
    created_at,
    updated_at,
    category_key,
    default_movement_type,
    budgetable,
    technical
)
SELECT
    NULL,
    NULL,
    name,
    type,
    scope,
    TRUE,
    now(),
    now(),
    category_key,
    default_movement_type,
    budgetable,
    technical
FROM roots
    ON CONFLICT (name, type) WHERE profile_id IS NULL
    DO UPDATE SET
    scope = EXCLUDED.scope,
           active = TRUE,
           updated_at = now(),
           category_key = EXCLUDED.category_key,
           default_movement_type = EXCLUDED.default_movement_type,
           budgetable = EXCLUDED.budgetable,
           technical = EXCLUDED.technical;

WITH seed(parent_key, category_key, name, type, scope, default_movement_type, budgetable, technical) AS (
    VALUES
        -- Ingresos laborales
        ('ingresos_laborales', 'sueldo', 'Sueldo', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresos_laborales', 'aguinaldo', 'Aguinaldo', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresos_laborales', 'bonos_y_comisiones', 'Bonos y comisiones', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresos_laborales', 'horas_extra', 'Horas extra', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),

        -- Ingresos profesionales
        ('ingresos_profesionales', 'honorarios', 'Honorarios', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresos_profesionales', 'freelance', 'Freelance', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresos_profesionales', 'servicios_profesionales', 'Servicios profesionales', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),

        -- Ingresos negocio
        ('ingresos_negocio', 'ventas', 'Ventas', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresos_negocio', 'cobros_de_clientes', 'Cobros de clientes', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresos_negocio', 'recupero_de_gastos', 'Recupero de gastos', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),

        -- Ingresos financieros/eventuales
        ('ingresos_financieros', 'intereses_ganados', 'Intereses ganados', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresos_financieros', 'rendimientos', 'Rendimientos', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresos_financieros', 'dividendos', 'Dividendos', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresos_eventuales', 'beneficios_y_promociones', 'Beneficios y promociones', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresos_eventuales', 'devoluciones_y_reintegros', 'Devoluciones y reintegros', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresos_eventuales', 'transferencias_recibidas', 'Transferencias recibidas', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresos_eventuales', 'ayuda_familiar', 'Ayuda familiar', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresos_eventuales', 'venta_de_usados', 'Venta de usados', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('ingresos_eventuales', 'otros_ingresos', 'Otros ingresos', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),

        -- CJ ingresos
        ('cj_prestamos_ingresos', 'cj_interes_cobrado', 'CJ - Interés cobrado', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('cj_prestamos_ingresos', 'cj_mora_cobrada', 'CJ - Mora cobrada', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),
        ('cj_prestamos_ingresos', 'cj_comision_cobrada', 'CJ - Comisión cobrada', 'INCOME', 'GLOBAL', 'INCOME', TRUE, FALSE),

        -- Vivienda
        ('vivienda', 'alquiler', 'Alquiler', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('vivienda', 'expensas', 'Expensas', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('vivienda', 'hipoteca', 'Hipoteca', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('vivienda', 'abl', 'ABL', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('vivienda', 'impuesto_inmobiliario', 'Impuesto inmobiliario', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),

        -- Servicios
        ('servicios', 'electricidad', 'Electricidad', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('servicios', 'gas', 'Gas', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('servicios', 'agua', 'Agua', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('servicios', 'internet', 'Internet', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('servicios', 'telefonia_movil', 'Telefonía móvil', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('servicios', 'cable', 'Cable', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('servicios', 'streaming', 'Streaming', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),

        -- Salud fija
        ('salud_fija', 'prepaga', 'Prepaga', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('salud_fija', 'obra_social', 'Obra social', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('salud_fija', 'plan_medico', 'Plan médico', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),

        -- Educación fija
        ('educacion_fija', 'cuota_escolar', 'Cuota escolar', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('educacion_fija', 'universidad', 'Universidad', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('educacion_fija', 'cursos_recurrentes', 'Cursos recurrentes', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),

        -- Seguros
        ('seguros', 'seguro_del_auto', 'Seguro del auto', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('seguros', 'seguro_del_hogar', 'Seguro del hogar', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('seguros', 'seguro_de_vida', 'Seguro de vida', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('seguros', 'seguro_de_caucion', 'Seguro de caución', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),

        -- Suscripciones fijas
        ('suscripciones_fijas', 'software', 'Software', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('suscripciones_fijas', 'gimnasio', 'Gimnasio', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('suscripciones_fijas', 'plataformas_digitales', 'Plataformas digitales', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('suscripciones_fijas', 'herramientas_de_trabajo', 'Herramientas de trabajo', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),

        -- Impuestos fijos
        ('impuestos_fijos', 'monotributo', 'Monotributo', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('impuestos_fijos', 'autonomos', 'Autónomos', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('impuestos_fijos', 'ingresos_brutos', 'Ingresos Brutos', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('impuestos_fijos', 'iva', 'IVA', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('impuestos_fijos', 'ganancias', 'Ganancias', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('impuestos_fijos', 'patentes', 'Patentes', 'FIXED_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),

        -- Alimentación
        ('alimentacion', 'supermercado', 'Supermercado', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('alimentacion', 'almacen', 'Almacén', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('alimentacion', 'verduleria', 'Verdulería', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('alimentacion', 'carniceria', 'Carnicería', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('alimentacion', 'delivery', 'Delivery', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('alimentacion', 'restaurantes', 'Restaurantes', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),

        -- Transporte
        ('transporte', 'transporte_publico', 'Transporte público', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('transporte', 'combustible', 'Combustible', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('transporte', 'taxi_y_apps', 'Taxi y apps', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('transporte', 'estacionamiento', 'Estacionamiento', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('transporte', 'peajes', 'Peajes', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('transporte', 'mantenimiento_vehicular', 'Mantenimiento vehicular', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),

        -- Hogar
        ('hogar', 'limpieza', 'Limpieza', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('hogar', 'reparaciones', 'Reparaciones', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('hogar', 'ferreteria', 'Ferretería', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('hogar', 'muebles_y_decoracion', 'Muebles y decoración', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('hogar', 'electrodomesticos', 'Electrodomésticos', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),

        -- Salud variable
        ('salud_variable', 'medicamentos', 'Medicamentos', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('salud_variable', 'consultas_medicas', 'Consultas médicas', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('salud_variable', 'estudios_medicos', 'Estudios médicos', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('salud_variable', 'odontologia', 'Odontología', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('salud_variable', 'terapia', 'Terapia', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),

        -- Familia e hijos
        ('familia_e_hijos', 'colegio_materiales', 'Colegio y materiales', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('familia_e_hijos', 'actividades_de_hijos', 'Actividades de hijos', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('familia_e_hijos', 'ropa_de_hijos', 'Ropa de hijos', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('familia_e_hijos', 'juguetes', 'Juguetes', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('familia_e_hijos', 'cumpleanos_y_eventos', 'Cumpleaños y eventos', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),

        -- Mascotas
        ('mascotas', 'alimento_de_mascotas', 'Alimento de mascotas', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('mascotas', 'veterinaria', 'Veterinaria', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('mascotas', 'accesorios_de_mascotas', 'Accesorios de mascotas', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),

        -- Indumentaria
        ('indumentaria', 'ropa', 'Ropa', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('indumentaria', 'calzado', 'Calzado', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('indumentaria', 'accesorios', 'Accesorios', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),

        -- Ocio
        ('ocio', 'salidas', 'Salidas', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('ocio', 'entretenimiento', 'Entretenimiento', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('ocio', 'regalos', 'Regalos', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('ocio', 'donaciones', 'Donaciones', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),

        -- Viajes
        ('viajes', 'pasajes', 'Pasajes', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('viajes', 'hospedaje', 'Hospedaje', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('viajes', 'excursiones', 'Excursiones', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('viajes', 'viaticos', 'Viáticos', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),

        -- Bancos y comisiones
        ('bancos_y_comisiones', 'comisiones_y_cargos', 'Comisiones y cargos', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('bancos_y_comisiones', 'comisiones_bancarias', 'Comisiones bancarias', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('bancos_y_comisiones', 'mantenimiento_de_cuenta', 'Mantenimiento de cuenta', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('bancos_y_comisiones', 'impuesto_debitos_creditos', 'Impuesto débitos y créditos', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('bancos_y_comisiones', 'intereses_por_financiacion', 'Intereses por financiación', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),

        -- Impuestos variables
        ('impuestos_variables', 'percepciones', 'Percepciones', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('impuestos_variables', 'retenciones', 'Retenciones', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('impuestos_variables', 'arca', 'ARCA', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('impuestos_variables', 'multas_y_recargos', 'Multas y recargos', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),

        -- Gastos generales
        ('gastos_generales', 'compras_varias', 'Compras varias', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('gastos_generales', 'gastos_menores', 'Gastos menores', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),
        ('gastos_generales', 'otros_gastos', 'Otros gastos', 'VARIABLE_EXPENSE', 'GLOBAL', 'EXPENSE', TRUE, FALSE),

        -- Ahorro
        ('ahorro', 'fondo_de_emergencia', 'Fondo de emergencia', 'SAVING', 'GLOBAL', 'SAVING', TRUE, FALSE),
        ('ahorro', 'ahorro_general', 'Ahorro general', 'SAVING', 'GLOBAL', 'SAVING', TRUE, FALSE),
        ('ahorro', 'objetivo_vacaciones', 'Objetivo vacaciones', 'SAVING', 'GLOBAL', 'SAVING', TRUE, FALSE),
        ('ahorro', 'objetivo_auto', 'Objetivo auto', 'SAVING', 'GLOBAL', 'SAVING', TRUE, FALSE),
        ('ahorro', 'objetivo_vivienda', 'Objetivo vivienda', 'SAVING', 'GLOBAL', 'SAVING', TRUE, FALSE),
        ('ahorro', 'educacion_futura', 'Educación futura', 'SAVING', 'GLOBAL', 'SAVING', TRUE, FALSE),

        -- Transferencias internas / fondeo
        ('transferencias_internas', 'cuenta_dni_debin', 'Cuenta DNI / DEBIN', 'SAVING', 'GLOBAL', 'TRANSFER', FALSE, TRUE),
        ('transferencias_internas', 'fondeo_mercadopago_transferencias_internas', 'Fondeo MercadoPago / transferencias internas', 'SAVING', 'GLOBAL', 'TRANSFER', FALSE, TRUE),
        ('transferencias_internas', 'transferencia_entre_cuentas', 'Transferencia entre cuentas', 'SAVING', 'GLOBAL', 'TRANSFER', FALSE, TRUE),
        ('transferencias_internas', 'movimiento_entre_billeteras', 'Movimiento entre billeteras', 'SAVING', 'GLOBAL', 'TRANSFER', FALSE, TRUE),

        -- Inversiones
        ('inversiones', 'dolares', 'Dólares', 'INVESTMENT', 'GLOBAL', 'SAVING', TRUE, FALSE),
        ('inversiones', 'plazo_fijo', 'Plazo fijo', 'INVESTMENT', 'GLOBAL', 'SAVING', TRUE, FALSE),
        ('inversiones', 'fondos_comunes', 'Fondos comunes', 'INVESTMENT', 'GLOBAL', 'SAVING', TRUE, FALSE),
        ('inversiones', 'cedears', 'CEDEARs', 'INVESTMENT', 'GLOBAL', 'SAVING', TRUE, FALSE),
        ('inversiones', 'acciones', 'Acciones', 'INVESTMENT', 'GLOBAL', 'SAVING', TRUE, FALSE),
        ('inversiones', 'bonos', 'Bonos', 'INVESTMENT', 'GLOBAL', 'SAVING', TRUE, FALSE),
        ('inversiones', 'cripto', 'Cripto', 'INVESTMENT', 'GLOBAL', 'SAVING', TRUE, FALSE),
        ('inversiones', 'rendimientos_reinvertidos', 'Rendimientos reinvertidos', 'INVESTMENT', 'GLOBAL', 'SAVING', TRUE, FALSE),

        -- CJ inversión
        ('cj_prestamos_inversion', 'cj_capital_prestado', 'CJ - Capital prestado', 'INVESTMENT', 'GLOBAL', 'ADJUSTMENT', TRUE, FALSE),
        ('cj_prestamos_inversion', 'cj_capital_recuperado', 'CJ - Capital recuperado', 'INVESTMENT', 'GLOBAL', 'SAVING', TRUE, FALSE),
        ('cj_prestamos_inversion', 'cj_ajuste_de_prestamo', 'CJ - Ajuste de préstamo', 'INVESTMENT', 'GLOBAL', 'ADJUSTMENT', TRUE, FALSE),

        -- Deudas
        ('deudas', 'tarjeta_de_credito', 'Tarjeta de crédito', 'DEBT', 'GLOBAL', 'ADJUSTMENT', TRUE, FALSE),
        ('deudas', 'creditos_y_financiacion', 'Créditos y financiación', 'DEBT', 'GLOBAL', 'ADJUSTMENT', TRUE, FALSE),
        ('deudas', 'prestamo_personal', 'Préstamo personal', 'DEBT', 'GLOBAL', 'ADJUSTMENT', TRUE, FALSE),
        ('deudas', 'prestamo_familiar', 'Préstamo familiar', 'DEBT', 'GLOBAL', 'ADJUSTMENT', TRUE, FALSE),
        ('deudas', 'mercado_credito', 'Mercado Crédito', 'DEBT', 'GLOBAL', 'ADJUSTMENT', TRUE, FALSE),
        ('deudas', 'cuotas_pendientes', 'Cuotas pendientes', 'DEBT', 'GLOBAL', 'ADJUSTMENT', TRUE, FALSE),
        ('deudas', 'intereses_y_mora', 'Intereses y mora', 'DEBT', 'GLOBAL', 'ADJUSTMENT', TRUE, FALSE),
        ('deudas', 'refinanciacion', 'Refinanciación', 'DEBT', 'GLOBAL', 'ADJUSTMENT', TRUE, FALSE),
        ('deudas', 'cancelacion_de_deuda', 'Cancelación de deuda', 'DEBT', 'GLOBAL', 'ADJUSTMENT', TRUE, FALSE),

        -- Técnicas
        ('operaciones_tecnicas', 'ajuste_de_saldo', 'Ajuste de saldo', 'VARIABLE_EXPENSE', 'GLOBAL', 'ADJUSTMENT', FALSE, TRUE),
        ('operaciones_tecnicas', 'diferencia_por_redondeo', 'Diferencia por redondeo', 'VARIABLE_EXPENSE', 'GLOBAL', 'ADJUSTMENT', FALSE, TRUE),
        ('operaciones_tecnicas', 'movimiento_ignorado', 'Movimiento ignorado', 'VARIABLE_EXPENSE', 'GLOBAL', 'ADJUSTMENT', FALSE, TRUE),
        ('operaciones_tecnicas', 'reclasificacion_manual', 'Reclasificación manual', 'VARIABLE_EXPENSE', 'GLOBAL', 'ADJUSTMENT', FALSE, TRUE)
)
INSERT INTO category (
    profile_id,
    parent_id,
    name,
    type,
    scope,
    active,
    created_at,
    updated_at,
    category_key,
    default_movement_type,
    budgetable,
    technical
)
SELECT
    NULL,
    parent.id,
    child.name,
    child.type,
    child.scope,
    TRUE,
    now(),
    now(),
    child.category_key,
    child.default_movement_type,
    child.budgetable,
    child.technical
FROM seed child
         JOIN category parent
              ON parent.profile_id IS NULL
                  AND parent.category_key = child.parent_key
                  AND parent.type = child.type
    ON CONFLICT (name, type) WHERE profile_id IS NULL
    DO UPDATE SET
    parent_id = EXCLUDED.parent_id,
           scope = EXCLUDED.scope,
           active = TRUE,
           updated_at = now(),
           category_key = EXCLUDED.category_key,
           default_movement_type = EXCLUDED.default_movement_type,
           budgetable = EXCLUDED.budgetable,
           technical = EXCLUDED.technical;