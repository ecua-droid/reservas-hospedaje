package com.umg.hospedaje.exception;

/** El registro buscado (o la relación indicada) no existe -> HTTP 404. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
