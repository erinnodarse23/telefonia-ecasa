-- ============================================================
-- 05_add_unique_consumo.sql
-- Agrega UNIQUE(equipo_id, anio, mes) a la tabla consumo
-- ============================================================

-- 1. Eliminar duplicados (conserva la fila con menor id para cada combinación)
DELETE FROM consumo c1
USING consumo c2
WHERE c1.id > c2.id
  AND c1.equipo_id = c2.equipo_id
  AND c1.anio = c2.anio
  AND c1.mes = c2.mes;

-- 2. Agregar constraint UNIQUE
ALTER TABLE consumo
ADD CONSTRAINT uq_consumo_equipo_mes UNIQUE (equipo_id, anio, mes);

-- 3. Verificar
SELECT 'UNIQUE constraint agregada correctamente' AS resultado;
