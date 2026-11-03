-- Tabla para revocar JWT al cerrar sesión.
CREATE TABLE IF NOT EXISTS tokens_invalidados (
  token_jti VARCHAR(100) PRIMARY KEY,
  usuario_id INTEGER REFERENCES usuario(id) ON DELETE CASCADE,
  expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_tokens_invalidados_expires_at
  ON tokens_invalidados(expires_at);
