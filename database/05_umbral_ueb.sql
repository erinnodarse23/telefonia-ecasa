-- 05_umbral_ueb.sql
-- Agrega columna umbral_sobregasto a la tabla ueb para configuración por UEB

ALTER TABLE ueb ADD COLUMN IF NOT EXISTS umbral_sobregasto INTEGER DEFAULT NULL;
