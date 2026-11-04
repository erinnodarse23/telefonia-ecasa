-- ============================================================
-- 10_tokens_invalidados_v2.sql
-- Reemplaza el schema de tokens_invalidados por el nuevo del Node:
--   - id SERIAL PK
--   - token_jti VARCHAR(255) NOT NULL UNIQUE
--   - invalidado_en TIMESTAMP (auditoría)
--   - expires_at TIMESTAMP
-- Idempotente con DROP+CREATE (los tokens revocados son efímeros).
-- ⚠️ APLICAR DESPUÉS de verificar que el código Java actual sigue funcionando
-- ============================================================
DROP TABLE IF EXISTS tokens_invalidados CASCADE;

CREATE TABLE tokens_invalidados (
  id SERIAL PRIMARY KEY,
  token_jti VARCHAR(255) NOT NULL UNIQUE,
  usuario_id INTEGER REFERENCES usuario(id) ON DELETE CASCADE,
  invalidado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expires_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_tokens_invalidados_jti ON tokens_invalidados(token_jti);
CREATE INDEX idx_tokens_invalidados_exp ON tokens_invalidados(expires_at);

SELECT 'tokens_invalidados v2 OK' AS resultado;
