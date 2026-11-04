-- ============================================================
-- 05_add_unique_consumo.sql
-- Agrega UNIQUE(equipo_id, anio, mes) a la tabla consumo
-- Idempotente: se puede ejecutar múltiples veces
-- ============================================================
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_consumo_equipo_mes') THEN
    DELETE FROM consumo a USING consumo b
      WHERE a.id > b.id AND a.equipo_id = b.equipo_id AND a.anio = b.anio AND a.mes = b.mes;
    ALTER TABLE consumo ADD CONSTRAINT uq_consumo_equipo_mes UNIQUE (equipo_id, anio, mes);
  END IF;
END $$;

SELECT 'uq_consumo_equipo_mes OK' AS resultado;
