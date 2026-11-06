package com.umg.hospedaje.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Formato único de las respuestas de error de la API.
 *
 * @param fecha   momento del error
 * @param estado  código HTTP (400, 404, 409, 422, 500...)
 * @param error   título corto del error
 * @param mensaje explicación comprensible para el usuario
 * @param ruta    endpoint que se llamó
 * @param errores errores por campo (solo en validaciones; si no hay, va vacío)
 */
public record ErrorResponseDTO(
        LocalDateTime fecha,
        int estado,
        String error,
        String mensaje,
        String ruta,
        Map<String, String> errores
) {
}
