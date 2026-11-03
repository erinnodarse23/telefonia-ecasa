-- ============================================================
-- Script: 03_migrar_plan_anual.sql
-- Descripción: Crea la tabla plan_anual y migra datos desde
--              configuracion (tipo = 'PLAN_ANUAL') sin duplicar
--              ni eliminar datos existentes.
-- Orden de ejecución: después de 01_full_schema.sql y 02_seed_data.sql
-- Idempotente: se puede ejecutar múltiples veces sin riesgo.
-- ============================================================

BEGIN;

-- 1. Crear tabla plan_anual (si no existe)
CREATE TABLE IF NOT EXISTS plan_anual (
    id       SERIAL        PRIMARY KEY,
    ueb_id   INTEGER       NOT NULL REFERENCES ueb(id) ON DELETE CASCADE,
    anio     INTEGER       NOT NULL,
    monto    NUMERIC(12,2) NOT NULL,
    CONSTRAINT uq_plan_anual_ueb_anio UNIQUE (ueb_id, anio)
);

-- 2. Migrar datos desde configuracion sin duplicados
-- Usa #>> para extraer texto sin comillas JSON sea cual sea el formato
INSERT INTO plan_anual (ueb_id, anio, monto)
SELECT
    c.ueb_id,
    c.anio,
    COALESCE(
        NULLIF((c.valor_json::jsonb #>> '{monto}'), ''),  -- JSON objeto {monto: ...}
        NULLIF((c.valor_json::jsonb #>> '{}'), ''),        -- JSON escalar (número o string)
        '0'                                                -- Fallback seguro
    )::NUMERIC(12,2) AS monto
FROM configuracion c
WHERE c.tipo IN ('PLAN_ANUAL', 'plan_anual')
  AND c.ueb_id IS NOT NULL
  AND c.anio IS NOT NULL
ON CONFLICT (ueb_id, anio) DO NOTHING;

-- 3. Índices adicionales para rendimiento (si no existen)
CREATE INDEX IF NOT EXISTS idx_plan_anual_ueb_id ON plan_anual(ueb_id);
CREATE INDEX IF NOT EXISTS idx_plan_anual_anio   ON plan_anual(anio);

COMMIT;
