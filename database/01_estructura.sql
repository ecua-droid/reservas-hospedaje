-- ============================================================
-- Reservas de habitaciones - Paso 1: estructura de la base de datos
-- ------------------------------------------------------------
-- Base de datos: reservas_hospedaje (PostgreSQL 18)
-- Ejecutar en pgAdmin con el Query Tool abierto sobre "reservas_hospedaje".
-- ATENCION: elimina y vuelve a crear las tablas (se pierden los datos).
-- ============================================================

-- Extension incluida en PostgreSQL. Permite combinar "=" y rangos de fechas
-- en la restriccion EXCLUDE que impide reservas traslapadas (regla 2).
CREATE EXTENSION IF NOT EXISTS btree_gist;

DROP TABLE IF EXISTS reserva;
DROP TABLE IF EXISTS habitacion;

-- ------------------------------------------------------------
-- Entidad principal de apoyo: HABITACION
-- ------------------------------------------------------------
CREATE TABLE habitacion (
    id            BIGINT        GENERATED ALWAYS AS IDENTITY,
    numero        VARCHAR(10)   NOT NULL,
    tipo          VARCHAR(20)   NOT NULL,
    capacidad     INTEGER       NOT NULL,
    tarifa_noche  NUMERIC(10,2) NOT NULL,
    descripcion   VARCHAR(200),
    activa        BOOLEAN       NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_habitacion           PRIMARY KEY (id),
    CONSTRAINT uk_habitacion_numero    UNIQUE (numero),
    CONSTRAINT ck_habitacion_tipo      CHECK (tipo IN ('SENCILLA', 'DOBLE', 'TRIPLE', 'SUITE')),
    CONSTRAINT ck_habitacion_capacidad CHECK (capacidad BETWEEN 1 AND 10),
    CONSTRAINT ck_habitacion_tarifa    CHECK (tarifa_noche > 0)
);

-- ------------------------------------------------------------
-- Entidad relacionada: RESERVA (N reservas -> 1 habitacion)
-- ------------------------------------------------------------
CREATE TABLE reserva (
    id                  BIGINT        GENERATED ALWAYS AS IDENTITY,
    habitacion_id       BIGINT        NOT NULL,
    nombre_huesped      VARCHAR(100)  NOT NULL,
    documento_huesped   VARCHAR(20)   NOT NULL,
    telefono_huesped    VARCHAR(15),
    cantidad_huespedes  INTEGER       NOT NULL,
    fecha_entrada       DATE          NOT NULL,
    fecha_salida        DATE          NOT NULL,
    noches              INTEGER       NOT NULL,
    tarifa_noche        NUMERIC(10,2) NOT NULL,
    total               NUMERIC(12,2) NOT NULL,
    estado              VARCHAR(20)   NOT NULL DEFAULT 'ACTIVA',
    fecha_registro      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_reserva            PRIMARY KEY (id),
    CONSTRAINT fk_reserva_habitacion FOREIGN KEY (habitacion_id) REFERENCES habitacion (id),

    -- Regla 1: la fecha de salida debe ser posterior a la de entrada
    CONSTRAINT ck_reserva_fechas     CHECK (fecha_salida > fecha_entrada),
    CONSTRAINT ck_reserva_noches     CHECK (noches = fecha_salida - fecha_entrada),

    -- Regla 3: total = noches x tarifa
    CONSTRAINT ck_reserva_total      CHECK (total = noches * tarifa_noche),

    CONSTRAINT ck_reserva_huespedes  CHECK (cantidad_huespedes > 0),
    CONSTRAINT ck_reserva_tarifa     CHECK (tarifa_noche > 0),
    CONSTRAINT ck_reserva_estado     CHECK (estado IN ('ACTIVA', 'CANCELADA', 'FINALIZADA')),

    -- Regla 2: dos reservas ACTIVAS de la misma habitacion no pueden tener
    -- rangos de fechas que se crucen. '[)' = incluye la entrada y excluye la salida,
    -- asi un huesped puede entrar el mismo dia que otro sale.
    CONSTRAINT ex_reserva_sin_traslape EXCLUDE USING gist (
        habitacion_id WITH =,
        daterange(fecha_entrada, fecha_salida, '[)') WITH &&
    ) WHERE (estado = 'ACTIVA')
);

-- Indices para las consultas mas usadas
CREATE INDEX ix_reserva_habitacion ON reserva (habitacion_id);
CREATE INDEX ix_reserva_estado     ON reserva (estado);
CREATE INDEX ix_reserva_documento  ON reserva (documento_huesped);
