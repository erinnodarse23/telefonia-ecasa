-- ============================================================
-- 10_tokens_invalidados_v2.sql
-- Reemplaza el schema de tokens_invalidados por el nuevo schema:
--   - id SERIAL PK
--   - token_jti VARCHAR(255) NOT NULL UNIQUE
--   - invalidado_en TIMESTAMP (auditorÃ­a)
--   - expires_at TIMESTAMP
-- Idempotente con DROP+CREATE (los tokens revocados son efÃ­meros).
-- âš ï¸ APLICAR DESPUÃ‰S de verificar que el cÃ³digo Java actual sigue funcionando
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
