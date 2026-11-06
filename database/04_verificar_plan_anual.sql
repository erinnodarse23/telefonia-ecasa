-- ============================================================
-- Script: 04_verificar_plan_anual.sql
-- DescripciÃ³n: Verifica que la migraciÃ³n de PLAN_ANUAL desde
--              configuracion â†’ plan_anual se realizÃ³ correctamente.
-- Idempotente: se puede ejecutar mÃºltiples veces.
-- ============================================================

-- ============================================================
-- 1. Total de registros en plan_anual por aÃ±o
-- ============================================================
SELECT '1. Registros en plan_anual por aÃ±o' AS " ";

SELECT anio,
       COUNT(*)                              AS total_planes,
       TO_CHAR(SUM(monto), 'FM999,999,999.00') AS monto_total
FROM plan_anual
GROUP BY anio
ORDER BY anio;

-- ============================================================
-- 2. ComparaciÃ³n: configuracion vs plan_anual
-- ============================================================
SELECT '2. ComparaciÃ³n configuracion vs plan_anual' AS " ";

WITH
planes_en_config AS (
    SELECT COUNT(*) AS total
    FROM configuracion
    WHERE tipo IN ('PLAN_ANUAL', 'plan_anual')
      AND ueb_id IS NOT NULL
      AND anio IS NOT NULL
),
planes_en_migrados AS (
    SELECT COUNT(*) AS total FROM plan_anual
)
SELECT
    (SELECT total FROM planes_en_config)  AS en_configuracion,
    (SELECT total FROM planes_en_migrados) AS en_plan_anual,
    CASE
        WHEN (SELECT total FROM planes_en_config) = (SELECT total FROM planes_en_migrados)
        THEN 'OK - Coinciden'
        ELSE 'DIFERENCIA - Revisar'
    END                                   AS estado;

-- ============================================================
-- 3. UEB con plan en configuracion pero NO en plan_anual
-- ============================================================
SELECT '3. UEB faltantes en plan_anual' AS " ";

SELECT c.ueb_id,
       u.codigo,
       u.nombre,
       c.anio,
       c.valor_json AS plan_en_config
FROM configuracion c
JOIN ueb u ON u.id = c.ueb_id
WHERE c.tipo IN ('PLAN_ANUAL', 'plan_anual')
  AND c.ueb_id IS NOT NULL
  AND c.anio IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM plan_anual p
      WHERE p.ueb_id = c.ueb_id
        AND p.anio   = c.anio
  )
ORDER BY u.codigo, c.anio;

-- ============================================================
-- 4. Resumen final
-- ============================================================
SELECT '4. Resumen final' AS " ";

SELECT
    CASE
        WHEN (SELECT COUNT(*) FROM plan_anual) > 0
        THEN 'MigraciÃ³n completada. plan_anual tiene ' || COUNT(*)::TEXT || ' registros.'
        ELSE 'ADVERTENCIA: plan_anual estÃ¡ vacÃ­a.'
    END AS resultado
FROM plan_anual;

