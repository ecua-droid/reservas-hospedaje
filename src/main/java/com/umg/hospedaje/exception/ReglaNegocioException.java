package com.umg.hospedaje.exception;

/**
 * Los datos tienen formato correcto, pero la operación incumple una regla del negocio
 * (fecha de salida no posterior, capacidad excedida, reserva ya cancelada...) -> HTTP 422.
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
