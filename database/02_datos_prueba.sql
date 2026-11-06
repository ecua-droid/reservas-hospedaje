-- ============================================================
-- Reservas de habitaciones - Paso 2: datos de prueba
-- ------------------------------------------------------------
-- Ejecutar despues de 01_estructura.sql, sobre "reservas_hospedaje".
-- Borra los datos actuales y vuelve a cargar los de prueba.
--
-- Las fechas se calculan a partir de CURRENT_DATE (hoy), por eso
-- siempre hay reservas pasadas, en curso y futuras sin importar
-- el dia en que se ejecute el script.
-- Todos los nombres, documentos y telefonos son ficticios.
-- ============================================================

TRUNCATE TABLE reserva, habitacion RESTART IDENTITY;

-- ------------------------------------------------------------
-- 10 habitaciones (la 302 esta desactivada por remodelacion)
-- IDs resultantes: 101=1, 102=2, 103=3, 104=4, 201=5,
--                  202=6, 203=7, 204=8, 301=9, 302=10
-- ------------------------------------------------------------
INSERT INTO habitacion (numero, tipo, capacidad, tarifa_noche, descripcion, activa) VALUES
    ('101', 'SENCILLA', 1, 250.00, 'Cama individual, baño privado, primer nivel', TRUE),
    ('102', 'SENCILLA', 1, 250.00, 'Cama individual con vista al jardín',          TRUE),
    ('103', 'DOBLE',    2, 350.00, 'Cama matrimonial y baño privado',              TRUE),
    ('104', 'DOBLE',    2, 375.00, 'Dos camas individuales y balcón',              TRUE),
    ('201', 'DOBLE',    2, 350.00, 'Cama matrimonial, segundo nivel',              TRUE),
    ('202', 'TRIPLE',   3, 450.00, 'Tres camas individuales',                      TRUE),
    ('203', 'TRIPLE',   3, 475.00, 'Cama matrimonial más cama individual',         TRUE),
    ('204', 'SUITE',    4, 650.00, 'Suite familiar con sala y cocineta',           TRUE),
    ('301', 'SUITE',    4, 700.00, 'Suite con terraza y vista a los volcanes',     TRUE),
    ('302', 'DOBLE',    2, 325.00, 'En remodelación',                              FALSE);

-- ------------------------------------------------------------
-- 15 reservas. Noches, tarifa y total se calculan igual que en Java:
--   noches = salida - entrada,  total = noches * tarifa de la habitacion
-- ------------------------------------------------------------
INSERT INTO reserva (habitacion_id, nombre_huesped, documento_huesped, telefono_huesped,
                     cantidad_huespedes, fecha_entrada, fecha_salida,
                     noches, tarifa_noche, total, estado)
SELECT h.id,
       d.nombre,
       d.documento,
       d.telefono,
       d.huespedes,
       d.entrada,
       d.salida,
       d.salida - d.entrada,
       h.tarifa_noche,
       (d.salida - d.entrada) * h.tarifa_noche,
       d.estado
FROM (VALUES
    --  #  hab.   huesped                 documento     telefono     pers. entrada              salida               estado
    ( 1, '101', 'Ana Lucía Pérez',      'DPI-100001', '5550-1001', 1, CURRENT_DATE - 20, CURRENT_DATE - 17, 'FINALIZADA'),
    ( 2, '103', 'Carlos Méndez',        'DPI-100002', '5550-1002', 2, CURRENT_DATE - 15, CURRENT_DATE - 12, 'FINALIZADA'),
    ( 3, '204', 'María José Ramírez',   'DPI-100003', '5550-1003', 4, CURRENT_DATE - 10, CURRENT_DATE - 6,  'FINALIZADA'),
    ( 4, '202', 'José Castillo',        'DPI-100004', '5550-1004', 3, CURRENT_DATE - 8,  CURRENT_DATE - 5,  'CANCELADA'),
    ( 5, '103', 'Lucía Morales',        'DPI-100005', '5550-1005', 2, CURRENT_DATE - 2,  CURRENT_DATE + 2,  'ACTIVA'),
    ( 6, '201', 'Pedro Gómez',          'DPI-100006', '5550-1006', 2, CURRENT_DATE - 1,  CURRENT_DATE + 3,  'ACTIVA'),
    ( 7, '301', 'Sofía Hernández',      'DPI-100007', '5550-1007', 3, CURRENT_DATE,      CURRENT_DATE + 4,  'ACTIVA'),
    ( 8, '101', 'Ana Lucía Pérez',      'DPI-100001', '5550-1001', 1, CURRENT_DATE + 3,  CURRENT_DATE + 5,  'ACTIVA'),
    ( 9, '104', 'Diego Rodríguez',      'DPI-100008', NULL,        2, CURRENT_DATE + 5,  CURRENT_DATE + 8,  'ACTIVA'),
    (10, '202', 'Valeria López',        'DPI-100009', '5550-1009', 3, CURRENT_DATE + 7,  CURRENT_DATE + 10, 'ACTIVA'),
    (11, '103', 'Andrés Torres',        'DPI-100010', '5550-1010', 1, CURRENT_DATE + 2,  CURRENT_DATE + 6,  'ACTIVA'),
    (12, '204', 'Gabriela Cruz',        'DPI-100011', '5550-1011', 4, CURRENT_DATE + 10, CURRENT_DATE + 14, 'ACTIVA'),
    (13, '201', 'Ana Lucía Pérez',      'DPI-100001', '5550-1001', 2, CURRENT_DATE + 12, CURRENT_DATE + 15, 'CANCELADA'),
    (14, '102', 'Roberto Díaz',         'DPI-100012', NULL,        1, CURRENT_DATE + 1,  CURRENT_DATE + 4,  'CANCELADA'),
    (15, '203', 'Carmen Soto',          'DPI-100013', '5550-1013', 3, CURRENT_DATE + 15, CURRENT_DATE + 18, 'ACTIVA')
) AS d(orden, numero, nombre, documento, telefono, huespedes, entrada, salida, estado)
JOIN habitacion h ON h.numero = d.numero
ORDER BY d.orden;

-- Comprobacion rapida
SELECT 'habitaciones' AS tabla, COUNT(*) AS registros FROM habitacion
UNION ALL
SELECT 'reservas', COUNT(*) FROM reserva;
