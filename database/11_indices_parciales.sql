-- ============================================================
-- 11_indices_parciales.sql
-- Impone a nivel BD las validaciones que antes solo estaban en código:
--   - Un equipo NO puede tener 2 asignaciones activas
--   - Un empleado NO puede tener 2 asignaciones activas
--   - Un equipo NO puede tener 2 planes activos
-- ⚠️ Antes de aplicar, verificar duplicados con las queries al final
-- ============================================================

-- Pre-verificación: si hay duplicados, NO se aplican los índices
DO $$
DECLARE
  v_dup_equipo INT;
  v_dup_empleado INT;
  v_dup_plan INT;
BEGIN
  SELECT COUNT(*) INTO v_dup_equipo FROM (
    SELECT equipo_id FROM asignacion WHERE fecha_fin IS NULL GROUP BY equipo_id HAVING COUNT(*) > 1
  ) x;
  SELECT COUNT(*) INTO v_dup_empleado FROM (
    SELECT empleado_id FROM asignacion WHERE fecha_fin IS NULL GROUP BY empleado_id HAVING COUNT(*) > 1
  ) x;
  SELECT COUNT(*) INTO v_dup_plan FROM (
    SELECT equipo_id FROM plan WHERE fecha_fin IS NULL GROUP BY equipo_id HAVING COUNT(*) > 1
  ) x;

  IF v_dup_equipo > 0 OR v_dup_empleado > 0 OR v_dup_plan > 0 THEN
    RAISE EXCEPTION 'Hay duplicados activos: equipos_asig=%, empleados_asig=%, equipos_plan=%. Limpiar primero.',
      v_dup_equipo, v_dup_empleado, v_dup_plan;
  END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_asignacion_activa_equipo
  ON asignacion(equipo_id) WHERE fecha_fin IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_asignacion_activa_empleado
  ON asignacion(empleado_id) WHERE fecha_fin IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_plan_activo_equipo
  ON plan(equipo_id) WHERE fecha_fin IS NULL;

SELECT '3 índices únicos parciales OK' AS resultado;
