-- TEST: Verificar atribución histórica correcta
-- Escenario: equipo 53001001 se reasigna de NC a ADM en junio 2025

BEGIN;

-- 1. Datos actuales
SELECT 'ANTES' AS fase,
       (SELECT u.codigo FROM ueb u
        JOIN empleado emp ON emp.ueb_id = u.id
        JOIN asignacion a ON a.empleado_id = emp.id AND a.fecha_fin IS NULL
        JOIN equipo e ON e.id = a.equipo_id
        WHERE e.numero_linea = '53001001') AS ueb_actual;

-- 2. Simular reasignación: cerrar asignación actual del equipo 53001001
UPDATE asignacion
SET fecha_fin = '2025-06-01'
WHERE equipo_id = (SELECT id FROM equipo WHERE numero_linea = '53001001')
  AND fecha_fin IS NULL;

-- 3. Crear nueva asignación al empleado de UEB ADM
INSERT INTO asignacion (equipo_id, empleado_id, fecha_inicio, estado_equipo_entrega)
SELECT
    (SELECT id FROM equipo WHERE numero_linea = '53001001'),
    (SELECT id FROM empleado WHERE ueb_id = (SELECT id FROM ueb WHERE codigo = 'ADM') LIMIT 1),
    '2025-06-01',
    'bueno';

-- 4. Verificar atribución histórica
SELECT 'DESPUÉS' AS fase,
       (SELECT u.codigo FROM ueb u
        JOIN empleado emp ON emp.ueb_id = u.id
        JOIN asignacion a ON a.empleado_id = emp.id AND a.fecha_fin IS NULL
        JOIN equipo e ON e.id = a.equipo_id
        WHERE e.numero_linea = '53001001') AS ueb_actual;

-- 5. Consumo enero (debe seguir apareciendo en NC)
SELECT 'Consumo enero 2025 UEB NC (debe ser 420)' AS check_,
       COALESCE(SUM(c.voz_consumido), 0) AS voz_enero
FROM consumo c
JOIN equipo e ON e.id = c.equipo_id
JOIN asignacion a ON a.equipo_id = e.id
    AND a.fecha_inicio <= make_date(c.anio, c.mes, 1)
    AND (a.fecha_fin IS NULL OR a.fecha_fin >= make_date(c.anio, c.mes, 1))
JOIN empleado emp ON emp.id = a.empleado_id
WHERE c.anio = 2025 AND c.mes = 1
  AND emp.ueb_id = (SELECT id FROM ueb WHERE codigo = 'NC')
  AND e.numero_linea = '53001001';

-- 6. Consumo julio (debe aparecer en ADM, si hubiera datos)
SELECT 'Consumo julio 2025 UEB ADM' AS check_,
       COALESCE(SUM(c.voz_consumido), 0) AS voz_julio
FROM consumo c
JOIN equipo e ON e.id = c.equipo_id
JOIN asignacion a ON a.equipo_id = e.id
    AND a.fecha_inicio <= make_date(c.anio, c.mes, 1)
    AND (a.fecha_fin IS NULL OR a.fecha_fin >= make_date(c.anio, c.mes, 1))
JOIN empleado emp ON emp.id = a.empleado_id
WHERE c.anio = 2025 AND c.mes = 7
  AND emp.ueb_id = (SELECT id FROM ueb WHERE codigo = 'ADM')
  AND e.numero_linea = '53001001';

ROLLBACK;  -- ⚠️ Deshacer cambios de prueba

COMMIT;