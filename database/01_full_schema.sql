-- ============================================================
-- SISTEMA DE CONTROL DE TELEFONÍA MÓVIL - ECASA
-- Esquema completo de base de datos PostgreSQL
-- ============================================================
-- ORDEN DE CREACIÓN (respetando dependencias FK):
--   1. ueb
--   2. empleado  (FK → ueb)
--   3. equipo
--   4. usuario   (FK → empleado)
--   5. asignacion (FK → equipo, empleado)
--   6. plan      (FK → equipo)
--   7. consumo   (FK → equipo)
--   8. configuracion (FK → ueb)
--   9. traza     (FK → usuario)
-- ============================================================

-- ============================================================
-- 1. ueb — Unidades Empresariales de Base
-- ============================================================
CREATE TABLE IF NOT EXISTS ueb (
  id                SERIAL       PRIMARY KEY,
  codigo            VARCHAR(10)  NOT NULL UNIQUE,
  nombre            VARCHAR(150) NOT NULL,
  descripcion       TEXT,
  activo            BOOLEAN      DEFAULT TRUE,
  umbral_sobregasto INTEGER      DEFAULT NULL
);

CREATE INDEX IF NOT EXISTS idx_ueb_codigo ON ueb(codigo);
CREATE INDEX IF NOT EXISTS idx_ueb_activo ON ueb(activo);

-- ============================================================
-- 2. empleado — Empleados de la organización
-- ============================================================
CREATE TABLE IF NOT EXISTS empleado (
  id           SERIAL       PRIMARY KEY,
  nombre       VARCHAR(200) NOT NULL,
  cargo        VARCHAR(100),
  departamento VARCHAR(100),
  telefono     VARCHAR(20),
  ueb_id       INTEGER      REFERENCES ueb(id),
  activo       BOOLEAN      DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_empleado_ueb    ON empleado(ueb_id);
CREATE INDEX IF NOT EXISTS idx_empleado_activo ON empleado(activo);

-- ============================================================
-- 3. equipo — Equipos telefónicos
-- ============================================================
CREATE TABLE IF NOT EXISTS equipo (
  id            SERIAL       PRIMARY KEY,
  numero_linea  VARCHAR(20)  NOT NULL UNIQUE,
  marca         VARCHAR(100),
  modelo        VARCHAR(100),
  estado_fisico VARCHAR(20)  DEFAULT 'bueno'
    CHECK (estado_fisico IN ('nuevo', 'bueno', 'dañado', 'de baja')),
  observaciones TEXT,
  activo        BOOLEAN      DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_equipo_estado ON equipo(estado_fisico);
CREATE INDEX IF NOT EXISTS idx_equipo_activo ON equipo(activo);

-- ============================================================
-- 4. usuario — Usuarios del sistema (login)
-- ============================================================
CREATE TABLE IF NOT EXISTS usuario (
  id            SERIAL       PRIMARY KEY,
  login         VARCHAR(50)  NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  rol           VARCHAR(30)  NOT NULL
    CHECK (rol IN ('Administrador', 'GrupoComunicaciones', 'JefeUnidad')),
  empleado_id   INTEGER      UNIQUE REFERENCES empleado(id),
  activo        BOOLEAN      DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_usuario_login ON usuario(login);
CREATE INDEX IF NOT EXISTS idx_usuario_rol   ON usuario(rol);
CREATE INDEX IF NOT EXISTS idx_usuario_activo ON usuario(activo);

-- ============================================================
-- 5. asignacion — Asignación de equipos a empleados
-- ============================================================
CREATE TABLE IF NOT EXISTS asignacion (
  id                         SERIAL       PRIMARY KEY,
  equipo_id                  INTEGER      NOT NULL REFERENCES equipo(id),
  empleado_id                INTEGER      NOT NULL REFERENCES empleado(id),
  fecha_inicio               DATE         NOT NULL DEFAULT CURRENT_DATE,
  fecha_fin                  DATE,
  estado_equipo_entrega      VARCHAR(20),
  estado_equipo_devolucion   VARCHAR(20)
);

CREATE INDEX IF NOT EXISTS idx_asignacion_equipo    ON asignacion(equipo_id);
CREATE INDEX IF NOT EXISTS idx_asignacion_empleado  ON asignacion(empleado_id);
CREATE INDEX IF NOT EXISTS idx_asignacion_activa    ON asignacion(fecha_fin);

-- ============================================================
-- 6. plan — Planes tarifarios por equipo
-- ============================================================
CREATE TABLE IF NOT EXISTS plan (
  id          SERIAL       PRIMARY KEY,
  equipo_id   INTEGER      NOT NULL REFERENCES equipo(id),
  voz_plan    INTEGER,
  datos_plan  INTEGER,
  sms_plan    INTEGER,
  cuota       NUMERIC(10,2),
  fecha_inicio DATE        NOT NULL DEFAULT CURRENT_DATE,
  fecha_fin   DATE
);

CREATE INDEX IF NOT EXISTS idx_plan_equipo ON plan(equipo_id);

-- ============================================================
-- 7. consumo — Consumo mensual por equipo
-- ============================================================
CREATE TABLE IF NOT EXISTS consumo (
  id               SERIAL       PRIMARY KEY,
  equipo_id        INTEGER      NOT NULL REFERENCES equipo(id),
  anio             INTEGER      NOT NULL,
  mes              INTEGER      NOT NULL CHECK (mes BETWEEN 1 AND 12),
  voz_consumido    NUMERIC(10,2) DEFAULT 0,
  datos_consumido  NUMERIC(10,2) DEFAULT 0,
  sms_consumido    NUMERIC(10,2) DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_consumo_equipo    ON consumo(equipo_id);
CREATE INDEX IF NOT EXISTS idx_consumo_anio_mes  ON consumo(anio, mes);
CREATE INDEX IF NOT EXISTS idx_consumo_anio_equipo ON consumo(anio, equipo_id);

-- ============================================================
-- 8. configuracion — Parámetros del sistema (RF-20)
-- ============================================================
CREATE TABLE IF NOT EXISTS configuracion (
  id          SERIAL       PRIMARY KEY,
  tipo        VARCHAR(30)  NOT NULL,
  -- Ej: 'PLAN_ANUAL', 'DATOS_ANTERIORES', 'UMBRAL', 'limite_consumo'
  anio        INTEGER,
  ueb_id      INTEGER      REFERENCES ueb(id) ON DELETE CASCADE,
  valor_json  JSONB        NOT NULL,
  -- Valor almacenado como JSONB nativo
  created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT unique_config UNIQUE (tipo, anio, ueb_id)
);

CREATE INDEX IF NOT EXISTS idx_config_tipo_anio ON configuracion(tipo, anio);
CREATE INDEX IF NOT EXISTS idx_config_ueb       ON configuracion(ueb_id);

-- ============================================================
-- 9. traza — Auditoría de acciones (RF-19)
-- ============================================================
CREATE TABLE IF NOT EXISTS traza (
  id          SERIAL       PRIMARY KEY,
  usuario_id  INTEGER      REFERENCES usuario(id) ON DELETE SET NULL,
  accion      VARCHAR(50)  NOT NULL,
  -- Ej: 'CREATE', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT'
  entidad     VARCHAR(50),
  -- Nombre de la tabla afectada, ej: 'UEB', 'Empleado', 'Equipo', 'Usuario'
  entidad_id  INTEGER,
  -- ID del registro afectado
  descripcion TEXT,
  ip          INET,
  -- Dirección IP del cliente
  fecha       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  hora        TIME         DEFAULT CURRENT_TIME
);

CREATE INDEX IF NOT EXISTS idx_traza_usuario ON traza(usuario_id);
CREATE INDEX IF NOT EXISTS idx_traza_fecha   ON traza(fecha);
CREATE INDEX IF NOT EXISTS idx_traza_accion  ON traza(accion);

-- ============================================================
-- 10. alerta — Alertas automáticas de sobregasto (RF-21)
-- ============================================================
CREATE TABLE IF NOT EXISTS alerta (
  id            SERIAL       PRIMARY KEY,
  ueb_id        INTEGER      NOT NULL REFERENCES ueb(id) ON DELETE CASCADE,
  anio          INTEGER      NOT NULL,
  mes           INTEGER      NOT NULL CHECK (mes BETWEEN 1 AND 12),
  tipo          VARCHAR(30)  NOT NULL DEFAULT 'SOBREGASTO',
  porcentaje    NUMERIC(8,2),
  mensaje       TEXT         NOT NULL,
  leida         BOOLEAN      DEFAULT FALSE,
  descartada    BOOLEAN      DEFAULT FALSE,
  creada_en     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT unique_alerta UNIQUE (ueb_id, anio, mes, tipo)
);

CREATE INDEX IF NOT EXISTS idx_alerta_ueb       ON alerta(ueb_id);
CREATE INDEX IF NOT EXISTS idx_alerta_anio_mes  ON alerta(anio, mes);
CREATE INDEX IF NOT EXISTS idx_alerta_leida     ON alerta(leida);
CREATE INDEX IF NOT EXISTS idx_alerta_creada    ON alerta(creada_en);

-- ============================================================
-- FIN DEL ESQUEMA
-- ============================================================
