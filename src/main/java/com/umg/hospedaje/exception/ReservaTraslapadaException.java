package com.umg.hospedaje.exception;

/** Regla 2: la habitación ya tiene una reserva activa en esas fechas -> HTTP 409. */
public class ReservaTraslapadaException extends RuntimeException {

    public ReservaTraslapadaException(String mensaje) {
        super(mensaje);
    }
}
