-- ============================================================
-- Script: 04_verificar_estructura.sql
-- Descripción: Verifica que todas las tablas tengan las
--              columnas esperadas según el diseño. Si falta
--              alguna, genera el ALTER TABLE correspondiente.
-- Idempotente: se puede ejecutar múltiples veces.
-- ============================================================

DO $$
DECLARE
    v_rec   RECORD;
    v_cols  TEXT[];
    v_col   TEXT;
    v_count INTEGER := 0;
    v_sql   TEXT;
BEGIN

    -- ============================================================
    -- Configuración: tabla → columnas esperadas (ordenadas)
    -- ============================================================

    CREATE TEMP TABLE _expected (
        table_name VARCHAR(50),
        column_name VARCHAR(50),
        col_type   VARCHAR(50)
    ) ON COMMIT DROP;

    INSERT INTO _expected VALUES
        -- 1. UEB
        ('ueb',           'id',            'INTEGER'),
        ('ueb',           'codigo',        'VARCHAR'),
        ('ueb',           'nombre',        'VARCHAR'),
        ('ueb',           'descripcion',   'TEXT'),
        ('ueb',           'activo',        'BOOLEAN'),
        -- 2. Empleado
        ('empleado',      'id',            'INTEGER'),
        ('empleado',      'nombre',        'VARCHAR'),
        ('empleado',      'cargo',         'VARCHAR'),
        ('empleado',      'departamento',  'VARCHAR'),
        ('empleado',      'telefono',      'VARCHAR'),
        ('empleado',      'ueb_id',        'INTEGER'),
        ('empleado',      'activo',        'BOOLEAN'),
        -- 3. Equipo
        ('equipo',        'id',            'INTEGER'),
        ('equipo',        'numero_linea',  'VARCHAR'),
        ('equipo',        'marca',         'VARCHAR'),
        ('equipo',        'modelo',        'VARCHAR'),
        ('equipo',        'estado_fisico', 'VARCHAR'),
        ('equipo',        'observaciones', 'TEXT'),
        ('equipo',        'activo',        'BOOLEAN'),
        -- 4. Usuario
        ('usuario',       'id',            'INTEGER'),
        ('usuario',       'login',         'VARCHAR'),
        ('usuario',       'password_hash', 'VARCHAR'),
        ('usuario',       'rol',           'VARCHAR'),
        ('usuario',       'empleado_id',   'INTEGER'),
        ('usuario',       'activo',        'BOOLEAN'),
        -- 5. Asignacion
        ('asignacion',    'id',            'INTEGER'),
        ('asignacion',    'equipo_id',     'INTEGER'),
        ('asignacion',    'empleado_id',   'INTEGER'),
        ('asignacion',    'fecha_inicio',  'DATE'),
        ('asignacion',    'fecha_fin',     'DATE'),
        ('asignacion',    'estado_equipo_entrega',    'VARCHAR'),
        ('asignacion',    'estado_equipo_devolucion', 'VARCHAR'),
        -- 6. Plan
        ('plan',          'id',            'INTEGER'),
        ('plan',          'equipo_id',     'INTEGER'),
        ('plan',          'voz_plan',      'INTEGER'),
        ('plan',          'datos_plan',    'INTEGER'),
        ('plan',          'sms_plan',      'INTEGER'),
        ('plan',          'cuota',         'NUMERIC'),
        ('plan',          'fecha_inicio',  'DATE'),
        ('plan',          'fecha_fin',     'DATE'),
        -- 7. Consumo
        ('consumo',       'id',            'INTEGER'),
        ('consumo',       'equipo_id',     'INTEGER'),
        ('consumo',       'anio',          'INTEGER'),
        ('consumo',       'mes',           'INTEGER'),
        ('consumo',       'voz_consumido',   'NUMERIC'),
        ('consumo',       'datos_consumido', 'NUMERIC'),
        ('consumo',       'sms_consumido',   'NUMERIC'),
        -- 8. Configuracion
        ('configuracion', 'id',            'INTEGER'),
        ('configuracion', 'tipo',          'VARCHAR'),
        ('configuracion', 'anio',          'INTEGER'),
        ('configuracion', 'ueb_id',        'INTEGER'),
        ('configuracion', 'valor_json',    'TEXT'),
        -- 9. Traza
        ('traza',         'id',            'INTEGER'),
        ('traza',         'usuario_id',    'INTEGER'),
        ('traza',         'fecha',         'TIMESTAMP'),
        ('traza',         'hora',          'TIME'),
        ('traza',         'accion',        'VARCHAR'),
        ('traza',         'entidad',       'VARCHAR'),
        ('traza',         'entidad_id',    'INTEGER'),
        ('traza',         'descripcion',   'TEXT'),
        ('traza',         'ip',            'INET');

    -- ============================================================
    -- Verificar cada tabla
    -- ============================================================

    RAISE NOTICE '============================================';
    RAISE NOTICE 'VERIFICACIÓN DE ESTRUCTURA DE TABLAS';
    RAISE NOTICE '============================================';
    RAISE NOTICE '';

    FOR v_rec IN
        SELECT DISTINCT e.table_name
        FROM _expected e
        ORDER BY e.table_name
    LOOP
        RAISE NOTICE '--- Tabla: % ---', v_rec.table_name;

        SELECT ARRAY_AGG(e.column_name ORDER BY e.column_name)
        INTO v_cols
        FROM _expected e
        WHERE e.table_name = v_rec.table_name;

        v_count := 0;

        FOREACH v_col IN ARRAY v_cols
        LOOP
            IF EXISTS (
                SELECT 1
                FROM information_schema.columns c
                WHERE c.table_name = v_rec.table_name
                  AND c.column_name = v_col
                  AND c.table_schema = current_schema()
            ) THEN
                -- Columna existe → OK (sin mensaje)
            ELSE
                v_count := v_count + 1;
                RAISE NOTICE '  FALTA: %.%', v_rec.table_name, v_col;

                -- Generar ALTER TABLE
                CASE
                    WHEN v_rec.table_name = 'traza' AND v_col = 'hora' THEN
                        v_sql := 'ALTER TABLE traza ADD COLUMN hora TIME DEFAULT CURRENT_TIME;';
                    WHEN v_rec.table_name = 'traza' AND v_col = 'ip' THEN
                        v_sql := 'ALTER TABLE traza ADD COLUMN ip INET;';
                    WHEN v_col IN ('activo') THEN
                        v_sql := format('ALTER TABLE %I ADD COLUMN %I BOOLEAN DEFAULT TRUE;', v_rec.table_name, v_col);
                    WHEN v_col IN ('descripcion', 'observaciones') THEN
                        v_sql := format('ALTER TABLE %I ADD COLUMN %I TEXT;', v_rec.table_name, v_col);
                    WHEN v_col IN ('monto') THEN
                        v_sql := format('ALTER TABLE %I ADD COLUMN %I NUMERIC(12,2) NOT NULL DEFAULT 0;', v_rec.table_name, v_col);
                    WHEN v_col LIKE '%_id' THEN
                        v_sql := format('ALTER TABLE %I ADD COLUMN %I INTEGER;', v_rec.table_name, v_col);
                    WHEN v_col IN ('fecha_inicio', 'fecha_fin') THEN
                        v_sql := format('ALTER TABLE %I ADD COLUMN %I DATE;', v_rec.table_name, v_col);
                    WHEN v_col IN ('fecha') THEN
                        v_sql := format('ALTER TABLE %I ADD COLUMN %I TIMESTAMP DEFAULT CURRENT_TIMESTAMP;', v_rec.table_name, v_col);
                    WHEN v_col IN ('anio', 'mes', 'entidad_id') THEN
                        v_sql := format('ALTER TABLE %I ADD COLUMN %I INTEGER;', v_rec.table_name, v_col);
                    WHEN v_col IN ('voz_consumido', 'datos_consumido', 'sms_consumido') THEN
                        v_sql := format('ALTER TABLE %I ADD COLUMN %I NUMERIC(10,2) DEFAULT 0;', v_rec.table_name, v_col);
                    WHEN v_col IN ('cuota') THEN
                        v_sql := format('ALTER TABLE %I ADD COLUMN %I NUMERIC(10,2);', v_rec.table_name, v_col);
                    WHEN v_col IN ('voz_plan', 'datos_plan', 'sms_plan') THEN
                        v_sql := format('ALTER TABLE %I ADD COLUMN %I INTEGER;', v_rec.table_name, v_col);
                    ELSE
                        v_sql := format('ALTER TABLE %I ADD COLUMN %I VARCHAR(255);', v_rec.table_name, v_col);
                END CASE;

                RAISE NOTICE '  >>> %', v_sql;
            END IF;
        END LOOP;

        IF v_count = 0 THEN
            RAISE NOTICE '  ✓ Todas las columnas presentes';
        END IF;
        RAISE NOTICE '';
    END LOOP;

    -- ============================================================
    -- Tablas que NO existen
    -- ============================================================
    FOR v_rec IN
        SELECT DISTINCT e.table_name
        FROM _expected e
        WHERE NOT EXISTS (
            SELECT 1 FROM information_schema.tables t
            WHERE t.table_name = e.table_name
              AND t.table_schema = current_schema()
        )
    LOOP
        RAISE NOTICE '--- Tabla faltante: % ---', v_rec.table_name;
        RAISE NOTICE '  >>> CREATE TABLE % (...); -- Debe crearse', v_rec.table_name;
        RAISE NOTICE '';
    END LOOP;

    -- ============================================================
    -- Advertencia especial para traza.hora
    -- ============================================================
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_name = 'traza' AND table_schema = current_schema()
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'traza'
          AND column_name = 'hora'
          AND table_schema = current_schema()
    ) THEN
        RAISE NOTICE '⚠  NOTA: traza tiene columna "fecha" (TIMESTAMP) que ya incluye';
        RAISE NOTICE '    fecha+hora. Si prefieres columnas separadas, ejecuta:';
        RAISE NOTICE '    >>> ALTER TABLE traza ADD COLUMN hora TIME DEFAULT CURRENT_TIME;';
        RAISE NOTICE '    >>> ALTER TABLE traza ALTER COLUMN fecha TYPE DATE USING fecha::DATE;';
        RAISE NOTICE '';
    END IF;

    -- ============================================================
    -- Resumen de tablas existentes
    -- ============================================================
    RAISE NOTICE '============================================';
    RAISE NOTICE 'TABLAS EXISTENTES EN EL ESQUEMA:';
    RAISE NOTICE '============================================';
    FOR v_rec IN
        SELECT table_name
        FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_type = 'BASE TABLE'
        ORDER BY table_name
    LOOP
        RAISE NOTICE '  - %', v_rec.table_name;
    END LOOP;

    DROP TABLE IF EXISTS _expected;

END $$;
