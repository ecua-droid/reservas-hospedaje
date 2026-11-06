package com.umg.hospedaje.exception;

/** Se intenta registrar un dato único que ya existe (número de habitación) -> HTTP 409. */
public class RegistroDuplicadoException extends RuntimeException {

    public RegistroDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
