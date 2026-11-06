package com.umg.hospedaje.exception;

/** Parámetros de consulta vacíos o fuera de rango -> HTTP 400. */
public class SolicitudInvalidaException extends RuntimeException {

    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
