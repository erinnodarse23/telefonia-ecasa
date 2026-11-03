-- ============================================================
-- 99_reset_and_seed.sql
-- Resetea la BD a un estado limpio conocido.
--
-- ⚠️  ESTE SCRIPT BORRA TODOS LOS DATOS.
--     Usar solo para desarrollo o demo.
-- ============================================================

BEGIN;

-- 1. Truncate en cascada (respeta FKs, resetea SERIAL)
TRUNCATE TABLE
    tokens_invalidados,
    traza,
    alerta,
    consumo,
    plan,
    asignacion,
    usuario,
    equipo,
    empleado,
    configuracion,
    plan_anual,
    ueb
RESTART IDENTITY CASCADE;

-- 2. Aplicar seed (se hace aparte con \i, no con INSERT inline,
--    para mantener el seed original en un archivo independiente)
-- ⚠️ Este bloque NO inserta datos; solo limpia.
--    Ejecutar 02_seed_data.sql después de este script.

COMMIT;