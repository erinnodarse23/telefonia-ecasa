-- ============================================================
-- 08_configuracion_upsert.sql
-- Corrige el UNIQUE constraint de configuracion para que funcione
-- correctamente con NULL en anio y ueb_id.
--
-- Estrategia: reemplazar UNIQUE constraint (roto con NULL) por
-- 3 índices únicos parciales (uno por cada caso de uso).
-- Idempotente: se puede ejecutar múltiples veces.
-- ============================================================

BEGIN;

-- 1. Eliminar el constraint UNIQUE antiguo (no funciona con NULL)
ALTER TABLE configuracion
    DROP CONSTRAINT IF EXISTS unique_config;

-- 2. Crear índices únicos parciales para cada caso

-- Caso 1: configuración global (sin año, sin UEB)
--   Ej: UMBRAL, límite de consumo global
CREATE UNIQUE INDEX IF NOT EXISTS uk_config_global
    ON configuracion (tipo)
    WHERE anio IS NULL AND ueb_id IS NULL;

-- Caso 2: configuración por año, sin UEB
--   Ej: DATOS_ANTERIORES por año
CREATE UNIQUE INDEX IF NOT EXISTS uk_config_anual
    ON configuracion (tipo, anio)
    WHERE anio IS NOT NULL AND ueb_id IS NULL;

-- Caso 3: configuración por año + UEB
--   Ej: PLAN_ANUAL por UEB y año, límite de consumo por UEB
CREATE UNIQUE INDEX IF NOT EXISTS uk_config_anual_ueb
    ON configuracion (tipo, anio, ueb_id)
    WHERE anio IS NOT NULL AND ueb_id IS NOT NULL;

COMMIT;

-- Verificación
SELECT '=== Índices únicos de configuracion ===' AS check_;
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'configuracion'
  AND indexname LIKE 'uk_config%'
ORDER BY indexname;

SELECT '=== Duplicados existentes (deben ser 0) ===' AS check_;
SELECT tipo, anio, ueb_id, COUNT(*) AS filas
FROM configuracion
GROUP BY tipo, anio, ueb_id
HAVING COUNT(*) > 1
ORDER BY tipo, anio NULLS FIRST, ueb_id NULLS FIRST;