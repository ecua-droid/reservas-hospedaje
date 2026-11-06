package com.umg.hospedaje.dto;

import com.umg.hospedaje.entity.EstadoReserva;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Indicador "total estimado de una reserva":
 * desglose noches x tarifa = total de una reserva registrada.
 */
public record TotalReservaDTO(
        Long reservaId,
        String numeroHabitacion,
        String nombreHuesped,
        LocalDate fechaEntrada,
        LocalDate fechaSalida,
        int noches,
        BigDecimal tarifaNoche,
        BigDecimal total,
        EstadoReserva estado,
        String detalle
) {
}
