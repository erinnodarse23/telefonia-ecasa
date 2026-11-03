-- ============================================================
-- 06_usuario_ueb_id.sql
-- Agrega columna ueb_id a usuario para alinear con el código
-- Java que asume acceso directo a la UEB del usuario.
-- Idempotente: se puede ejecutar múltiples veces.
-- ============================================================

BEGIN;

-- 1. Agregar columna ueb_id (referenciando ueb, ON DELETE SET NULL
--    para no bloquear el borrado de una UEB con usuarios asignados)
ALTER TABLE usuario
    ADD COLUMN IF NOT EXISTS ueb_id INTEGER REFERENCES ueb(id) ON DELETE SET NULL;

-- 2. Poblar ueb_id solo para usuarios con rol 'JefeUnidad'.
--    Administrador y GrupoComunicaciones quedan con ueb_id = NULL
--    (tienen acceso a todas las UEBs, igual que en UsuarioService).
UPDATE usuario u
SET ueb_id = e.ueb_id
FROM empleado e
WHERE u.empleado_id = e.id
  AND u.rol = 'JefeUnidad'
  AND u.ueb_id IS NULL;

-- 3. Índice para consultas por UEB (filtros de alertas, dashboard, etc.)
CREATE INDEX IF NOT EXISTS idx_usuario_ueb ON usuario(ueb_id);

-- 4. Verificación
SELECT
    id,
    login,
    rol,
    empleado_id,
    ueb_id,
    activo
FROM usuario
ORDER BY id;

COMMIT;