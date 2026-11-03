-- ============================================================
-- 99_test_dashboard_hol.sql
-- Verifica que el fix de atribución histórica devuelve los
-- consumos correctos por UEB (HOL debe ser 13482).
-- ============================================================

SELECT '=== Resumen por UEB (año 2025) ===' AS check_;

SELECT u.codigo,
       u.nombre,
       COALESCE(cons.total_voz, 0)                AS total_voz,
       COALESCE(cons.total_datos, 0)              AS total_datos,
       COALESCE(cons.total_voz + cons.total_datos, 0) AS total_consumido
FROM ueb u
LEFT JOIN (
    SELECT emp2.ueb_id,
           SUM(c.voz_consumido)   AS total_voz,
           SUM(c.datos_consumido) AS total_datos
    FROM consumo c
    JOIN equipo e2 ON e2.id = c.equipo_id
    JOIN asignacion a2 ON a2.equipo_id = e2.id
        AND a2.fecha_inicio <= (make_date(c.anio, c.mes, 1) + interval '1 month - 1 day')::date
        AND (a2.fecha_fin IS NULL OR a2.fecha_fin >= (make_date(c.anio, c.mes, 1) + interval '1 month - 1 day')::date)
    JOIN empleado emp2 ON emp2.id = a2.empleado_id
    WHERE c.anio = 2025
    GROUP BY emp2.ueb_id
) cons ON cons.ueb_id = u.id
WHERE u.activo = true
ORDER BY u.codigo;