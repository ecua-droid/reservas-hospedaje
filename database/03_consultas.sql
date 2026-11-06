-- ============================================================
-- Reservas de habitaciones - Consultas para evidencias
-- ------------------------------------------------------------
-- Las mismas consultas que resuelve la API, escritas en SQL.
-- Ejecutar una por una en el Query Tool (seleccionar y presionar F5).
-- ============================================================

-- 1. Relacion: cada reserva con su habitacion
SELECT r.id, h.numero AS habitacion, h.tipo, r.nombre_huesped,
       r.fecha_entrada, r.fecha_salida, r.noches, r.total, r.estado
FROM reserva r
JOIN habitacion h ON h.id = r.habitacion_id
ORDER BY r.fecha_entrada DESC;

-- 2. Consulta: reservas por huesped (nombre o documento)
SELECT r.id, r.nombre_huesped, r.documento_huesped, h.numero AS habitacion,
       r.fecha_entrada, r.fecha_salida, r.total, r.estado
FROM reserva r
JOIN habitacion h ON h.id = r.habitacion_id
WHERE r.nombre_huesped ILIKE '%ana%'
   OR r.documento_huesped = 'DPI-100001'
ORDER BY r.fecha_entrada DESC;

-- 3. Consulta: habitaciones disponibles por periodo
--    (ejemplo: entrar dentro de 3 dias y salir dentro de 6 dias, para 2 personas)
SELECT h.numero, h.tipo, h.capacidad, h.tarifa_noche,
       (CURRENT_DATE + 6) - (CURRENT_DATE + 3)                  AS noches,
       ((CURRENT_DATE + 6) - (CURRENT_DATE + 3)) * h.tarifa_noche AS total_estimado
FROM habitacion h
WHERE h.activa
  AND h.capacidad >= 2
  AND NOT EXISTS (
        SELECT 1
        FROM reserva r
        WHERE r.habitacion_id = h.id
          AND r.estado = 'ACTIVA'
          AND r.fecha_entrada < CURRENT_DATE + 6
          AND r.fecha_salida  > CURRENT_DATE + 3
  )
ORDER BY h.numero;

-- 4. Consulta: reservas activas
SELECT r.id, h.numero AS habitacion, r.nombre_huesped,
       r.fecha_entrada, r.fecha_salida, r.total
FROM reserva r
JOIN habitacion h ON h.id = r.habitacion_id
WHERE r.estado = 'ACTIVA'
ORDER BY r.fecha_entrada;

-- 5. Indicador: total estimado de una reserva (noches x tarifa)
SELECT r.id, h.numero AS habitacion, r.nombre_huesped,
       r.noches, r.tarifa_noche,
       r.noches * r.tarifa_noche AS total_calculado,
       r.total                   AS total_guardado
FROM reserva r
JOIN habitacion h ON h.id = r.habitacion_id
WHERE r.id = 5;

-- 6. Indicador: resumen por estado
SELECT estado, COUNT(*) AS cantidad, SUM(total) AS monto
FROM reserva
GROUP BY estado
ORDER BY estado;

-- 7. Indicador: ocupacion de hoy
SELECT COUNT(DISTINCT r.habitacion_id)                                AS ocupadas_hoy,
       (SELECT COUNT(*) FROM habitacion WHERE activa)                 AS habitaciones_activas,
       ROUND(COUNT(DISTINCT r.habitacion_id) * 100.0
             / (SELECT COUNT(*) FROM habitacion WHERE activa), 2)     AS porcentaje_ocupacion
FROM reserva r
WHERE r.estado = 'ACTIVA'
  AND r.fecha_entrada <= CURRENT_DATE
  AND r.fecha_salida  >  CURRENT_DATE;

-- 8. Prueba de la restriccion de la base de datos (DEBE FALLAR):
--    la habitacion 103 (id 3) ya tiene una reserva activa de hoy-2 a hoy+2.
--    Error esperado: "conflicting key value violates exclusion constraint ex_reserva_sin_traslape"
INSERT INTO reserva (habitacion_id, nombre_huesped, documento_huesped, cantidad_huespedes,
                     fecha_entrada, fecha_salida, noches, tarifa_noche, total, estado)
VALUES (3, 'Prueba Traslape', 'DPI-999999', 1,
        CURRENT_DATE, CURRENT_DATE + 1, 1, 350.00, 350.00, 'ACTIVA');

-- 9. Prueba de la regla 1 en la base de datos (DEBE FALLAR): salida igual a la entrada
--    Error esperado: "violates check constraint ck_reserva_fechas"
INSERT INTO reserva (habitacion_id, nombre_huesped, documento_huesped, cantidad_huespedes,
                     fecha_entrada, fecha_salida, noches, tarifa_noche, total, estado)
VALUES (2, 'Prueba Fechas', 'DPI-999998', 1,
        CURRENT_DATE + 30, CURRENT_DATE + 30, 0, 250.00, 0.00, 'ACTIVA');
