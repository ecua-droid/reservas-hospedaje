package com.umg.hospedaje.entity;

/**
 * Estados por los que pasa una reserva.
 *
 * ACTIVA     -> reservada o con el huésped hospedado. Es la única que ocupa la habitación.
 * CANCELADA  -> anulada antes de usarse (libera las fechas).
 * FINALIZADA -> el huésped ya hizo check-out.
 */
public enum EstadoReserva {
    ACTIVA,
    CANCELADA,
    FINALIZADA
}
