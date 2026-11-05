-- ============================================================
-- SCRIPT 05: Corrige discrepancias menores detectadas en la
--            auditorÃ­a del proyecto ECASA.
--
-- 1. traza: agrega columna hora (TIME) generada desde fecha
-- 2. configuracion: migra valor_json de TEXT a JSONB
-- ============================================================

-- ============================================================
-- 1. traza â€” Agregar columna hora separada (RF-19)
-- ============================================================
-- La columna fecha (TIMESTAMP) ya almacena fecha+hora, pero el
-- documento REQUISITOS especifica una columna hora separada.
-- Agregamos la columna y la poblaremos desde fecha::time.

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'traza' AND column_name = 'hora'
  ) THEN
    ALTER TABLE traza ADD COLUMN hora TIME;
    UPDATE traza SET hora = fecha::time WHERE hora IS NULL;
  END IF;
END $$;

-- ============================================================
-- 2. configuracion â€” Migrar valor_json de TEXT a JSONB
-- ============================================================
-- La app usaba TEXT + JSON.stringify/parse manual. Con JSONB
-- PostgreSQL permite operaciones nativas sobre el JSON.

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'configuracion'
      AND column_name = 'valor_json'
      AND data_type = 'text'
  ) THEN
    ALTER TABLE configuracion
      ALTER COLUMN valor_json TYPE JSONB
      USING valor_json::jsonb;
  END IF;
END $$;

-- Añadido por sync con Node (commit 2)
ALTER TABLE traza ALTER COLUMN hora SET DEFAULT CURRENT_TIME;
