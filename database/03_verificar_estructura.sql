-- ============================================================
-- Script: 03_verificar_estructura.sql
-- Verificación de solo lectura (SELECT) del esquema esperado.
-- Devuelve TRUE/FALSE por elemento. No modifica la BD.
-- ============================================================

SELECT 'usuario.ueb_id' AS verificacion,
       EXISTS (SELECT 1 FROM information_schema.columns
         WHERE table_schema = 'public' AND table_name = 'usuario' AND column_name = 'ueb_id') AS existe
UNION ALL
SELECT 'usuario.empleado_id',
       EXISTS (SELECT 1 FROM information_schema.columns
         WHERE table_schema = 'public' AND table_name = 'usuario' AND column_name = 'empleado_id')
UNION ALL
SELECT 'tokens_invalidados',
       EXISTS (SELECT 1 FROM information_schema.tables
         WHERE table_schema = 'public' AND table_name = 'tokens_invalidados' AND table_type = 'BASE TABLE')
UNION ALL
SELECT 'plan_anual',
       EXISTS (SELECT 1 FROM information_schema.tables
         WHERE table_schema = 'public' AND table_name = 'plan_anual' AND table_type = 'BASE TABLE')
UNION ALL
SELECT 'alerta',
       EXISTS (SELECT 1 FROM information_schema.tables
         WHERE table_schema = 'public' AND table_name = 'alerta' AND table_type = 'BASE TABLE')
UNION ALL
SELECT 'ueb.umbral_sobregasto',
       EXISTS (SELECT 1 FROM information_schema.columns
         WHERE table_schema = 'public' AND table_name = 'ueb' AND column_name = 'umbral_sobregasto')
UNION ALL
SELECT 'uq_consumo_equipo_mes',
       EXISTS (SELECT 1 FROM pg_constraint
         WHERE conname = 'uq_consumo_equipo_mes' AND conrelid = 'public.consumo'::regclass)
UNION ALL
SELECT 'idx_unique_asignacion_activa_equipo',
       EXISTS (SELECT 1 FROM pg_indexes
         WHERE schemaname = 'public' AND indexname = 'idx_unique_asignacion_activa_equipo')
UNION ALL
SELECT 'idx_unique_asignacion_activa_empleado',
       EXISTS (SELECT 1 FROM pg_indexes
         WHERE schemaname = 'public' AND indexname = 'idx_unique_asignacion_activa_empleado')
UNION ALL
SELECT 'idx_unique_plan_activo_equipo',
       EXISTS (SELECT 1 FROM pg_indexes
         WHERE schemaname = 'public' AND indexname = 'idx_unique_plan_activo_equipo')
ORDER BY verificacion;
