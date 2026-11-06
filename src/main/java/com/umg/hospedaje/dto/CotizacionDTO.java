package com.umg.hospedaje.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Total estimado ANTES de reservar: cuánto costaría una habitación en ciertas fechas
 * y si está disponible. No guarda nada en la base de datos.
 */
public record CotizacionDTO(
        Long habitacionId,
        String numeroHabitacion,
        LocalDate fechaEntrada,
        LocalDate fechaSalida,
        int noches,
        BigDecimal tarifaNoche,
        BigDecimal totalEstimado,
        boolean disponible,
        String mensaje
) {
}
