package com.umg.hospedaje.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Indicadores generales del hospedaje (conteos, porcentaje de ocupación y monto estimado).
 */
public record ResumenReservasDTO(
        LocalDate fecha,
        long totalHabitaciones,
        long habitacionesActivas,
        long habitacionesOcupadasHoy,
        BigDecimal porcentajeOcupacionHoy,
        long reservasActivas,
        long reservasCanceladas,
        long reservasFinalizadas,
        BigDecimal ingresoEstimadoActivas
) {
}
