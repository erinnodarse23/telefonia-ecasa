-- ============================================================
-- 09_remove_empleado_id.sql
-- Migra usuario.empleado_id -> usuario.ueb_id y luego DROPs empleado_id.
-- Idempotente: se puede ejecutar múltiples veces.
-- ⚠️ APLICAR DESPUÉS de refactorizar UsuarioRepository en Java
-- ============================================================
BEGIN;

ALTER TABLE usuario ADD COLUMN IF NOT EXISTS ueb_id INTEGER;

-- Migrar datos: solo si empleado_id aún existe
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns
             WHERE table_schema = 'public' AND table_name = 'usuario' AND column_name = 'empleado_id') THEN
    EXECUTE 'UPDATE usuario u SET ueb_id = e.ueb_id FROM empleado e
             WHERE u.empleado_id = e.id AND u.ueb_id IS NULL';
  END IF;
END $$;

-- FK e índice
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_usuario_ueb') THEN
    ALTER TABLE usuario ADD CONSTRAINT fk_usuario_ueb
      FOREIGN KEY (ueb_id) REFERENCES ueb(id) ON DELETE SET NULL;
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_usuario_ueb ON usuario(ueb_id);

-- DROP de empleado_id y sus constraints
ALTER TABLE usuario DROP CONSTRAINT IF EXISTS usuario_empleado_id_key;
ALTER TABLE usuario DROP CONSTRAINT IF EXISTS usuario_empleado_id_fkey;
ALTER TABLE usuario DROP COLUMN IF EXISTS empleado_id;

COMMIT;

SELECT 'usuario.empleado_id eliminado OK' AS resultado;
