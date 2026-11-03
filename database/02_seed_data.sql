-- ============================================================
-- SISTEMA DE CONTROL DE TELEFONÍA MÓVIL - ECASA
-- 02_seed_data.sql — Datos de prueba completos
-- ============================================================
-- ORDEN DE INSERCIÓN (respetando dependencias FK):
--   1. ueb
--   2. empleado  (FK → ueb)
--   3. equipo
--   4. usuario   (FK → empleado)
--   5. asignacion (FK → equipo, empleado)
--   6. plan      (FK → equipo)
--   7. consumo   (FK → equipo)
--   8. configuracion (FK → ueb)
--   9. plan_anual (FK → ueb)
-- ============================================================
-- CONTRASEÑAS (hashes bcrypt generados con bcryptjs):
--   admin123  → $2b$10$St6cRwyPouF6r0LPHX4PSu9GsS32gImxH2UhYBgJrN/8nihasBh.a
--   com123    → $2b$10$IF9gJMdZ5y14pm7Nn06sRuSEtzNaBzIm5D4tyx2LZ7LtFgUICqoNC
--   jefe123   → $2b$10$Mh56bimSwl/N580oKJ2Uo.D.dggHZNoLpHLsnGYAGu83meO15dx9.
-- ============================================================

BEGIN;

-- ============================================================
-- 1. UEB (11 unidades)
-- ============================================================
INSERT INTO ueb (codigo, nombre, descripcion) VALUES
('NC',  'Nivel Central',      'Dirección central de ECASA'),
('ADM', 'Administrativa',     'Unidad administrativa y de gestión'),
('LOG', 'Logística',          'Unidad de logística y suministros'),
('HAB', 'Habana',             'Unidad provincial La Habana'),
('GER', 'Gerona',             'Unidad municipal Gerona'),
('VAR', 'Varadero',           'Unidad municipal Varadero'),
('SCL', 'Santa Clara',        'Unidad provincial Santa Clara'),
('CCO', 'Cayo Coco',          'Unidad municipal Cayo Coco'),
('CAM', 'Camagüey',           'Unidad provincial Camagüey'),
('HOL', 'Holguín',            'Unidad provincial Holguín'),
('SCU', 'Santiago de Cuba',   'Unidad provincial Santiago de Cuba');

-- ============================================================
-- 2. EMPLEADOS (mínimo 2 por UEB = 27 en total)
-- ============================================================

-- Nivel Central (NC)
INSERT INTO empleado (nombre, cargo, departamento, telefono, ueb_id)
SELECT 'Carlos Mendoza López',     'Director General',         'Dirección General',   '52001001', id FROM ueb WHERE codigo='NC';
INSERT INTO empleado (nombre, cargo, departamento, telefono, ueb_id)
SELECT 'Marta Pérez Rodríguez',    'Jefe de Comunicaciones',   'Comunicaciones',      '52001002', id FROM ueb WHERE codigo='NC';
INSERT INTO empleado (nombre, cargo, departamento, telefono, ueb_id)
SELECT 'Laura González Torres',    'Analista de Sistemas',     'Tecnología',          '52001003', id FROM ueb WHERE codigo='NC';

-- Administrativa (ADM)
INSERT INTO empleado (nombre, cargo, departamento, telefono, ueb_id)
SELECT 'Luis Hernández Álvarez',   'Jefe de Unidad',           'Administración',      '52002001', id FROM ueb WHERE codigo='ADM';
INSERT INTO empleado (nombre, cargo, departamento, telefono, ueb_id)
SELECT 'Diana Martínez Reyes',     'Contadora',                'Contabilidad',        '52002002', id FROM ueb WHERE codigo='ADM';

-- Logística (LOG)
INSERT INTO empleado (nombre, cargo, departamento, telefono, ueb_id)
SELECT 'Jorge Luis García',        'Jefe de Unidad',           'Logística',           '52003001', id FROM ueb WHERE codigo='LOG';
INSERT INTO empleado (nombre, cargo, departamento, telefono, ueb_id)
SELECT 'Rosa María Pérez',         'Especialista en Compras',  'Compras',             '52003002', id FROM ueb WHERE codigo='LOG';
INSERT INTO empleado (nombre, cargo, departamento, telefono, ueb_id)
SELECT 'Antonio Vega Cruz',        'Almacenero',               'Almacén',             '52003003', id FROM ueb WHERE codigo='LOG';

-- Habana (HAB)
INSERT INTO empleado (nombre, cargo, departamento, telefono, ueb_id)
SELECT 'María del Carmen Díaz',    'Jefa de Unidad',           'Dirección Provincial','52004001', id FROM ueb WHERE codigo='HAB';
INSERT INTO empleado (nombre, cargo, departamento, telefono, ueb_id)
SELECT 'Alejandro González Cruz',  'Especialista en Redes',    'Redes',               '52004002', id FROM ueb WHERE codigo='HAB';

-- Gerona (GER)
INSERT INTO empleado (nombre, cargo, departamento, telefono, ueb_id)
SELECT 'Ramón Castillo Díaz',      'Jefe de Unidad',           'Dirección Municipal', '52005001', id FROM ueb WHERE codigo='GER';
INSERT INTO empleado (nombre, cargo, departamento, telefono, ueb_id)
SELECT 'Yolanda Medina Reyes',     'Especialista Técnica',     'Técnico',             '52005002', id FROM ueb WHERE codigo='GER';

-- Varadero (VAR)
INSERT INTO empleado (nombre, cargo, departamento, telefono, ueb_id)
SELECT 'Elena Torres Sánchez',     'Jefa de Unidad',           'Dirección Municipal', '52006001', id FROM ueb WHERE codigo='VAR';
INSERT INTO empleado (nombre, cargo, departamento, telefono, ueb_id)
SELECT 'Oscar Martínez González',  'Especialista en Redes',    'Redes',               '52006002', id FROM ueb WHERE codigo='VAR';

-- Santa Clara (SCL)
INSERT INTO empleado (nombre, cargo, departamento, telefono, ueb_id)
SELECT 'Víctor Hugo Álvarez',      'Jefe de Unidad',           'Dirección Provincial','52007001', id FROM ueb WHERE codigo='SCL';
INSERT INTO empleado (nombre, cargo, departamento, telefono, ueb_id)
SELECT 'Nora Jiménez Rodríguez',   'Especialista Técnica',     'Técnico',             '52007002', id FROM ueb WHERE codigo='SCL';

-- Cayo Coco (CCO)
INSERT INTO empleado (nombre, cargo, departamento, telefono, ueb_id)
SELECT 'Adriana López Hernández',  'Jefa de Unidad',           'Dirección Municipal', '52008001', id FROM ueb WHERE codigo='CCO';
INSERT INTO empleado (nombre, cargo, departamento, telefono, ueb_id)
SELECT 'Francisco Pérez Sánchez',  'Especialista en Redes',    'Redes',               '52008002', id FROM ueb WHERE codigo='CCO';

-- Camagüey (CAM)
INSERT INTO empleado (nombre, cargo, departamento, telefono, ueb_id)
SELECT 'Gilberto Torres Cruz',     'Jefe de Unidad',           'Dirección Provincial','52009001', id FROM ueb WHERE codigo='CAM';
INSERT INTO empleado (nombre, cargo, departamento, telefono, ueb_id)
SELECT 'Evelyn Díaz Martínez',     'Especialista Técnica',     'Técnico',             '52009002', id FROM ueb WHERE codigo='CAM';

-- Holguín (HOL)
INSERT INTO empleado (nombre, cargo, departamento, telefono, ueb_id)
SELECT 'Dolores Cruz Pérez',       'Jefa de Unidad',           'Dirección Provincial','52010001', id FROM ueb WHERE codigo='HOL';
INSERT INTO empleado (nombre, cargo, departamento, telefono, ueb_id)
SELECT 'Ernesto Sánchez Morales',  'Especialista en Redes',    'Redes',               '52010002', id FROM ueb WHERE codigo='HOL';

-- Santiago de Cuba (SCU)
INSERT INTO empleado (nombre, cargo, departamento, telefono, ueb_id)
SELECT 'Rolando Méndez Torres',    'Jefe de Unidad',           'Dirección Provincial','52011001', id FROM ueb WHERE codigo='SCU';
INSERT INTO empleado (nombre, cargo, departamento, telefono, ueb_id)
SELECT 'Lisset Pérez Rodríguez',   'Especialista Técnica',     'Técnico',             '52011002', id FROM ueb WHERE codigo='SCU';

-- ============================================================
-- 3. EQUIPOS (12 equipos con distintas marcas/modelos)
-- ============================================================
INSERT INTO equipo (numero_linea, marca, modelo, estado_fisico) VALUES ('53001001', 'Samsung',     'Galaxy A54',  'bueno');
INSERT INTO equipo (numero_linea, marca, modelo, estado_fisico) VALUES ('53001002', 'Samsung',     'Galaxy S23',  'bueno');
INSERT INTO equipo (numero_linea, marca, modelo, estado_fisico) VALUES ('53001003', 'Huawei',      'P60 Pro',     'nuevo');
INSERT INTO equipo (numero_linea, marca, modelo, estado_fisico) VALUES ('53001004', 'Huawei',      'P60 Pro',     'bueno');
INSERT INTO equipo (numero_linea, marca, modelo, estado_fisico) VALUES ('53001005', 'Xiaomi',      'Redmi Note 12','bueno');
INSERT INTO equipo (numero_linea, marca, modelo, estado_fisico) VALUES ('53001006', 'Xiaomi',      'Redmi Note 12','nuevo');
INSERT INTO equipo (numero_linea, marca, modelo, estado_fisico) VALUES ('53001007', 'Apple',       'iPhone 13',   'bueno');
INSERT INTO equipo (numero_linea, marca, modelo, estado_fisico) VALUES ('53001008', 'Apple',       'iPhone 13',   'dañado');
INSERT INTO equipo (numero_linea, marca, modelo, estado_fisico) VALUES ('53001009', 'Motorola',    'Edge 40',     'bueno');
INSERT INTO equipo (numero_linea, marca, modelo, estado_fisico) VALUES ('53001010', 'Motorola',    'Edge 40',     'nuevo');
INSERT INTO equipo (numero_linea, marca, modelo, estado_fisico) VALUES ('53001011', 'Samsung',     'Galaxy A14',  'bueno');
INSERT INTO equipo (numero_linea, marca, modelo, estado_fisico) VALUES ('53001012', 'Huawei',      'Nova 11i',    'bueno');

-- ============================================================
-- 4. USUARIOS (3 roles)
-- ============================================================
INSERT INTO usuario (login, password_hash, rol, empleado_id)
SELECT 'admin',          '$2b$10$St6cRwyPouF6r0LPHX4PSu9GsS32gImxH2UhYBgJrN/8nihasBh.a', 'Administrador',       id
FROM empleado WHERE nombre = 'Carlos Mendoza López';

INSERT INTO usuario (login, password_hash, rol, empleado_id)
SELECT 'comunicaciones', '$2b$10$IF9gJMdZ5y14pm7Nn06sRuSEtzNaBzIm5D4tyx2LZ7LtFgUICqoNC', 'GrupoComunicaciones', id
FROM empleado WHERE nombre = 'Marta Pérez Rodríguez';

INSERT INTO usuario (login, password_hash, rol, empleado_id)
SELECT 'jefe_nc',        '$2b$10$Mh56bimSwl/N580oKJ2Uo.D.dggHZNoLpHLsnGYAGu83meO15dx9.', 'JefeUnidad',          id
FROM empleado WHERE nombre = 'Laura González Torres';

-- ============================================================
-- 5. ASIGNACIONES (activas e históricas)
-- ============================================================

-- Activas (equipos asignados actualmente)
INSERT INTO asignacion (equipo_id, empleado_id, fecha_inicio, estado_equipo_entrega)
SELECT e.id, emp.id, '2024-01-15', 'bueno'
FROM equipo e, empleado emp
WHERE e.numero_linea = '53001001' AND emp.telefono = '52001001';

INSERT INTO asignacion (equipo_id, empleado_id, fecha_inicio, estado_equipo_entrega)
SELECT e.id, emp.id, '2024-01-15', 'bueno'
FROM equipo e, empleado emp
WHERE e.numero_linea = '53001002' AND emp.telefono = '52001002';

INSERT INTO asignacion (equipo_id, empleado_id, fecha_inicio, estado_equipo_entrega)
SELECT e.id, emp.id, '2024-02-01', 'nuevo'
FROM equipo e, empleado emp
WHERE e.numero_linea = '53001003' AND emp.telefono = '52003001';

INSERT INTO asignacion (equipo_id, empleado_id, fecha_inicio, estado_equipo_entrega)
SELECT e.id, emp.id, '2024-03-10', 'bueno'
FROM equipo e, empleado emp
WHERE e.numero_linea = '53001004' AND emp.telefono = '52004001';

INSERT INTO asignacion (equipo_id, empleado_id, fecha_inicio, estado_equipo_entrega)
SELECT e.id, emp.id, '2024-06-01', 'bueno'
FROM equipo e, empleado emp
WHERE e.numero_linea = '53001005' AND emp.telefono = '52005001';

INSERT INTO asignacion (equipo_id, empleado_id, fecha_inicio, estado_equipo_entrega)
SELECT e.id, emp.id, '2024-06-01', 'nuevo'
FROM equipo e, empleado emp
WHERE e.numero_linea = '53001006' AND emp.telefono = '52006001';

INSERT INTO asignacion (equipo_id, empleado_id, fecha_inicio, estado_equipo_entrega)
SELECT e.id, emp.id, '2024-07-15', 'bueno'
FROM equipo e, empleado emp
WHERE e.numero_linea = '53001007' AND emp.telefono = '52007001';

INSERT INTO asignacion (equipo_id, empleado_id, fecha_inicio, estado_equipo_entrega)
SELECT e.id, emp.id, '2024-09-01', 'bueno'
FROM equipo e, empleado emp
WHERE e.numero_linea = '53001009' AND emp.telefono = '52009001';

INSERT INTO asignacion (equipo_id, empleado_id, fecha_inicio, estado_equipo_entrega)
SELECT e.id, emp.id, '2025-01-10', 'nuevo'
FROM equipo e, empleado emp
WHERE e.numero_linea = '53001010' AND emp.telefono = '52010001';

-- Histórica (equipo devuelto)
INSERT INTO asignacion (equipo_id, empleado_id, fecha_inicio, fecha_fin, estado_equipo_entrega, estado_equipo_devolucion)
SELECT e.id, emp.id, '2023-06-01', '2024-05-30', 'bueno', 'dañado'
FROM equipo e, empleado emp
WHERE e.numero_linea = '53001008' AND emp.telefono = '52001001';

-- ============================================================
-- 6. PLANES (1 por equipo)
-- ============================================================
INSERT INTO plan (equipo_id, voz_plan, datos_plan, sms_plan, cuota, fecha_inicio)
SELECT e.id, 1000, 20480, 500, 1500.00, '2025-01-01'
FROM equipo e WHERE e.numero_linea = '53001001';

INSERT INTO plan (equipo_id, voz_plan, datos_plan, sms_plan, cuota, fecha_inicio)
SELECT e.id, 800, 10240, 300, 1200.00, '2025-01-01'
FROM equipo e WHERE e.numero_linea = '53001002';

INSERT INTO plan (equipo_id, voz_plan, datos_plan, sms_plan, cuota, fecha_inicio)
SELECT e.id, 600, 10240, 200, 1000.00, '2025-01-01'
FROM equipo e WHERE e.numero_linea = '53001003';

INSERT INTO plan (equipo_id, voz_plan, datos_plan, sms_plan, cuota, fecha_inicio)
SELECT e.id, 600, 5120,  200, 900.00,  '2025-01-01'
FROM equipo e WHERE e.numero_linea = '53001004';

INSERT INTO plan (equipo_id, voz_plan, datos_plan, sms_plan, cuota, fecha_inicio)
SELECT e.id, 500, 5120,  100, 800.00,  '2025-01-01'
FROM equipo e WHERE e.numero_linea = '53001005';

INSERT INTO plan (equipo_id, voz_plan, datos_plan, sms_plan, cuota, fecha_inicio)
SELECT e.id, 500, 5120,  100, 800.00,  '2025-01-01'
FROM equipo e WHERE e.numero_linea = '53001006';

INSERT INTO plan (equipo_id, voz_plan, datos_plan, sms_plan, cuota, fecha_inicio)
SELECT e.id, 1000, 20480, 500, 1500.00, '2025-01-01'
FROM equipo e WHERE e.numero_linea = '53001007';

INSERT INTO plan (equipo_id, voz_plan, datos_plan, sms_plan, cuota, fecha_inicio)
SELECT e.id, 400, 3072,  100, 600.00,  '2025-01-01'
FROM equipo e WHERE e.numero_linea = '53001009';

INSERT INTO plan (equipo_id, voz_plan, datos_plan, sms_plan, cuota, fecha_inicio)
SELECT e.id, 400, 3072,  100, 600.00,  '2025-01-01'
FROM equipo e WHERE e.numero_linea = '53001010';

INSERT INTO plan (equipo_id, voz_plan, datos_plan, sms_plan, cuota, fecha_inicio)
SELECT e.id, 500, 5120,  200, 850.00,  '2025-01-01'
FROM equipo e WHERE e.numero_linea = '53001011';

INSERT INTO plan (equipo_id, voz_plan, datos_plan, sms_plan, cuota, fecha_inicio)
SELECT e.id, 500, 5120,  200, 850.00,  '2025-01-01'
FROM equipo e WHERE e.numero_linea = '53001012';

-- ============================================================
-- 7. CONSUMOS (Enero - Abril 2025 por equipo)
-- ============================================================

-- 53001001 — NC Director General
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 1, 420, 5120, 85  FROM equipo e WHERE e.numero_linea = '53001001';
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 2, 380, 4800, 72  FROM equipo e WHERE e.numero_linea = '53001001';
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 3, 450, 5500, 90  FROM equipo e WHERE e.numero_linea = '53001001';
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 4, 390, 4900, 78  FROM equipo e WHERE e.numero_linea = '53001001';

-- 53001002 — NC Jefe Comunicaciones
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 1, 650, 8192, 120 FROM equipo e WHERE e.numero_linea = '53001002';
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 2, 580, 7600, 105 FROM equipo e WHERE e.numero_linea = '53001002';
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 3, 700, 9000, 140 FROM equipo e WHERE e.numero_linea = '53001002';
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 4, 620, 7800, 115 FROM equipo e WHERE e.numero_linea = '53001002';

-- 53001003 — LOG Jefe de Unidad
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 1, 300, 4096, 50  FROM equipo e WHERE e.numero_linea = '53001003';
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 2, 280, 3800, 45  FROM equipo e WHERE e.numero_linea = '53001003';
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 3, 350, 4500, 60  FROM equipo e WHERE e.numero_linea = '53001003';
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 4, 310, 4000, 48  FROM equipo e WHERE e.numero_linea = '53001003';

-- 53001004 — HAB Jefa de Unidad
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 1, 520, 4096, 95  FROM equipo e WHERE e.numero_linea = '53001004';
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 2, 480, 3800, 82  FROM equipo e WHERE e.numero_linea = '53001004';
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 3, 560, 4800, 110 FROM equipo e WHERE e.numero_linea = '53001004';
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 4, 500, 4200, 88  FROM equipo e WHERE e.numero_linea = '53001004';

-- 53001005 — GER Jefe de Unidad
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 1, 150, 2048, 25  FROM equipo e WHERE e.numero_linea = '53001005';
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 2, 130, 1800, 20  FROM equipo e WHERE e.numero_linea = '53001005';
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 3, 180, 2500, 30  FROM equipo e WHERE e.numero_linea = '53001005';
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 4, 160, 2200, 22  FROM equipo e WHERE e.numero_linea = '53001005';

-- 53001006 — VAR Jefa de Unidad
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 1, 200, 3072, 35  FROM equipo e WHERE e.numero_linea = '53001006';
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 2, 180, 2800, 30  FROM equipo e WHERE e.numero_linea = '53001006';
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 3, 220, 3500, 40  FROM equipo e WHERE e.numero_linea = '53001006';
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 4, 190, 3000, 32  FROM equipo e WHERE e.numero_linea = '53001006';

-- 53001007 — SCL Jefe de Unidad
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 1, 480, 6144, 75  FROM equipo e WHERE e.numero_linea = '53001007';
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 2, 430, 5600, 65  FROM equipo e WHERE e.numero_linea = '53001007';
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 3, 520, 7000, 90  FROM equipo e WHERE e.numero_linea = '53001007';
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 4, 460, 6000, 70  FROM equipo e WHERE e.numero_linea = '53001007';

-- 53001009 — CAM Jefe de Unidad
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 1, 250, 2560, 40  FROM equipo e WHERE e.numero_linea = '53001009';
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 2, 220, 2300, 35  FROM equipo e WHERE e.numero_linea = '53001009';
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 3, 280, 3000, 50  FROM equipo e WHERE e.numero_linea = '53001009';
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 4, 240, 2700, 38  FROM equipo e WHERE e.numero_linea = '53001009';

-- 53001010 — HOL Jefa de Unidad
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 1, 280, 3072, 45  FROM equipo e WHERE e.numero_linea = '53001010';
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 2, 250, 2800, 38  FROM equipo e WHERE e.numero_linea = '53001010';
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 3, 310, 3500, 55  FROM equipo e WHERE e.numero_linea = '53001010';
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 4, 270, 3000, 42  FROM equipo e WHERE e.numero_linea = '53001010';

-- 53001011 — SCU Jefe de Unidad
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 1, 320, 3584, 55  FROM equipo e WHERE e.numero_linea = '53001011';
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 2, 290, 3200, 48  FROM equipo e WHERE e.numero_linea = '53001011';
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 3, 360, 4000, 65  FROM equipo e WHERE e.numero_linea = '53001011';
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 4, 310, 3500, 50  FROM equipo e WHERE e.numero_linea = '53001011';

-- 53001012 — SCU Especialista Técnica
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 1, 180, 2048, 30  FROM equipo e WHERE e.numero_linea = '53001012';
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 2, 160, 1800, 25  FROM equipo e WHERE e.numero_linea = '53001012';
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 3, 200, 2500, 35  FROM equipo e WHERE e.numero_linea = '53001012';
INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido)
SELECT e.id, 2025, 4, 170, 2200, 28  FROM equipo e WHERE e.numero_linea = '53001012';

-- ============================================================
-- 8. CONFIGURACIONES
-- ============================================================

-- 8a. PLAN_ANUAL por UEB para 2025
INSERT INTO configuracion (tipo, anio, ueb_id, valor_json)
SELECT 'PLAN_ANUAL', 2025, id, '62400' FROM ueb WHERE codigo = 'NC';
INSERT INTO configuracion (tipo, anio, ueb_id, valor_json)
SELECT 'PLAN_ANUAL', 2025, id, '34800' FROM ueb WHERE codigo = 'ADM';
INSERT INTO configuracion (tipo, anio, ueb_id, valor_json)
SELECT 'PLAN_ANUAL', 2025, id, '33600' FROM ueb WHERE codigo = 'LOG';
INSERT INTO configuracion (tipo, anio, ueb_id, valor_json)
SELECT 'PLAN_ANUAL', 2025, id, '0'     FROM ueb WHERE codigo = 'HAB';
INSERT INTO configuracion (tipo, anio, ueb_id, valor_json)
SELECT 'PLAN_ANUAL', 2025, id, '0'     FROM ueb WHERE codigo = 'GER';
INSERT INTO configuracion (tipo, anio, ueb_id, valor_json)
SELECT 'PLAN_ANUAL', 2025, id, '0'     FROM ueb WHERE codigo = 'VAR';
INSERT INTO configuracion (tipo, anio, ueb_id, valor_json)
SELECT 'PLAN_ANUAL', 2025, id, '0'     FROM ueb WHERE codigo = 'SCL';
INSERT INTO configuracion (tipo, anio, ueb_id, valor_json)
SELECT 'PLAN_ANUAL', 2025, id, '0'     FROM ueb WHERE codigo = 'CCO';
INSERT INTO configuracion (tipo, anio, ueb_id, valor_json)
SELECT 'PLAN_ANUAL', 2025, id, '0'     FROM ueb WHERE codigo = 'CAM';
INSERT INTO configuracion (tipo, anio, ueb_id, valor_json)
SELECT 'PLAN_ANUAL', 2025, id, '0'     FROM ueb WHERE codigo = 'HOL';
INSERT INTO configuracion (tipo, anio, ueb_id, valor_json)
SELECT 'PLAN_ANUAL', 2025, id, '0'     FROM ueb WHERE codigo = 'SCU';

-- 8b. DATOS_ANTERIORES — totales mensuales empresa 2024
INSERT INTO configuracion (tipo, anio, ueb_id, valor_json)
VALUES ('DATOS_ANTERIORES', 2024, NULL,
  '{"totales_mensuales": [260000, 260000, 260000, 260000, 260000, 260000, 260000, 260000, 260000, 260000, 260000, 260000]}');

-- 8c. UMBRAL global de sobregasto
INSERT INTO configuracion (tipo, anio, ueb_id, valor_json)
VALUES ('UMBRAL', NULL, NULL, '{"porcentaje_sobregasto": 20}');

-- 8d. límites de consumo globales
INSERT INTO configuracion (tipo, anio, ueb_id, valor_json)
VALUES ('limite_consumo', 2025, NULL,
  '{"voz_max": 1000, "datos_max": 10240, "sms_max": 500}');

-- 8e. límites de consumo para NC
INSERT INTO configuracion (tipo, anio, ueb_id, valor_json)
SELECT 'limite_consumo', 2025, id, '{"voz_max": 800, "datos_max": 5120, "sms_max": 300}'
FROM ueb WHERE codigo = 'NC';

-- 8f. límites de consumo para HAB
INSERT INTO configuracion (tipo, anio, ueb_id, valor_json)
SELECT 'limite_consumo', 2025, id, '{"voz_max": 600, "datos_max": 3072, "sms_max": 200}'
FROM ueb WHERE codigo = 'HAB';

-- ============================================================
-- 9. PLAN ANUAL por UEB (coincide con configuracion.PLAN_ANUAL)
--     Usa ON CONFLICT DO NOTHING por si 03_migrar_plan_anual.sql
--     ya migró los datos.
-- ============================================================
INSERT INTO plan_anual (ueb_id, anio, monto)
SELECT id, 2025, 62400 FROM ueb WHERE codigo = 'NC'
ON CONFLICT (ueb_id, anio) DO NOTHING;
INSERT INTO plan_anual (ueb_id, anio, monto)
SELECT id, 2025, 34800 FROM ueb WHERE codigo = 'ADM'
ON CONFLICT (ueb_id, anio) DO NOTHING;
INSERT INTO plan_anual (ueb_id, anio, monto)
SELECT id, 2025, 33600 FROM ueb WHERE codigo = 'LOG'
ON CONFLICT (ueb_id, anio) DO NOTHING;
INSERT INTO plan_anual (ueb_id, anio, monto)
SELECT id, 2025, 0     FROM ueb WHERE codigo = 'HAB'
ON CONFLICT (ueb_id, anio) DO NOTHING;
INSERT INTO plan_anual (ueb_id, anio, monto)
SELECT id, 2025, 0     FROM ueb WHERE codigo = 'GER'
ON CONFLICT (ueb_id, anio) DO NOTHING;
INSERT INTO plan_anual (ueb_id, anio, monto)
SELECT id, 2025, 0     FROM ueb WHERE codigo = 'VAR'
ON CONFLICT (ueb_id, anio) DO NOTHING;
INSERT INTO plan_anual (ueb_id, anio, monto)
SELECT id, 2025, 0     FROM ueb WHERE codigo = 'SCL'
ON CONFLICT (ueb_id, anio) DO NOTHING;
INSERT INTO plan_anual (ueb_id, anio, monto)
SELECT id, 2025, 0     FROM ueb WHERE codigo = 'CCO'
ON CONFLICT (ueb_id, anio) DO NOTHING;
INSERT INTO plan_anual (ueb_id, anio, monto)
SELECT id, 2025, 0     FROM ueb WHERE codigo = 'CAM'
ON CONFLICT (ueb_id, anio) DO NOTHING;
INSERT INTO plan_anual (ueb_id, anio, monto)
SELECT id, 2025, 0     FROM ueb WHERE codigo = 'HOL'
ON CONFLICT (ueb_id, anio) DO NOTHING;
INSERT INTO plan_anual (ueb_id, anio, monto)
SELECT id, 2025, 0     FROM ueb WHERE codigo = 'SCU'
ON CONFLICT (ueb_id, anio) DO NOTHING;

-- ============================================================
COMMIT;

-- ============================================================
-- VERIFICACIÓN: mostrar resumen de datos insertados
-- ============================================================
SELECT 'ueb' AS tabla, COUNT(*) AS filas FROM ueb
UNION ALL
SELECT 'empleado', COUNT(*) FROM empleado
UNION ALL
SELECT 'equipo', COUNT(*) FROM equipo
UNION ALL
SELECT 'usuario', COUNT(*) FROM usuario
UNION ALL
SELECT 'asignacion', COUNT(*) FROM asignacion
UNION ALL
SELECT 'plan', COUNT(*) FROM plan
UNION ALL
SELECT 'consumo', COUNT(*) FROM consumo
UNION ALL
SELECT 'configuracion', COUNT(*) FROM configuracion
UNION ALL
SELECT 'plan_anual', COUNT(*) FROM plan_anual
ORDER BY tabla;
