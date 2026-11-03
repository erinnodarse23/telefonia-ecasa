-- ============================================================
-- 07_indices_dashboard.sql
-- Índices para optimizar las queries del dashboard con
-- atribución histórica por fecha de asignación.
-- Idempotente: se puede ejecutar múltiples veces.
-- ============================================================

BEGIN;

-- Acelera el JOIN por fecha de asignación
CREATE INDEX IF NOT EXISTS idx_asignacion_rango_fecha
    ON asignacion (equipo_id, fecha_inicio, fecha_fin);

-- Acelera la búsqueda de consumos por equipo y año
CREATE INDEX IF NOT EXISTS idx_consumo_equipo_anio_mes
    ON consumo (equipo_id, anio, mes);

COMMIT;

-- Verificación
SELECT indexname FROM pg_indexes
WHERE tablename IN ('asignacion', 'consumo')
  AND indexname IN ('idx_asignacion_rango_fecha', 'idx_consumo_equipo_anio_mes')
ORDER BY indexname;