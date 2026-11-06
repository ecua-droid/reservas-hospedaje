package com.umg.hospedaje.dto;

import com.umg.hospedaje.entity.EstadoReserva;
import com.umg.hospedaje.entity.Habitacion;
import com.umg.hospedaje.entity.Reserva;
import com.umg.hospedaje.entity.TipoHabitacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Información de una reserva que la API devuelve.
 * En lugar de la entidad Habitación completa, solo incluye su id, número y tipo.
 */
public record ReservaResponseDTO(
        Long id,
        Long habitacionId,
        String numeroHabitacion,
        TipoHabitacion tipoHabitacion,
        String nombreHuesped,
        String documentoHuesped,
        String telefonoHuesped,
        Integer cantidadHuespedes,
        LocalDate fechaEntrada,
        LocalDate fechaSalida,
        Integer noches,
        BigDecimal tarifaNoche,
        BigDecimal total,
        EstadoReserva estado,
        LocalDateTime fechaRegistro
) {

    /** Convierte la entidad en DTO. */
    public static ReservaResponseDTO desde(Reserva reserva) {
        Habitacion habitacion = reserva.getHabitacion();
        return new ReservaResponseDTO(
                reserva.getId(),
                habitacion.getId(),
                habitacion.getNumero(),
                habitacion.getTipo(),
                reserva.getNombreHuesped(),
                reserva.getDocumentoHuesped(),
                reserva.getTelefonoHuesped(),
                reserva.getCantidadHuespedes(),
                reserva.getFechaEntrada(),
                reserva.getFechaSalida(),
                reserva.getNoches(),
                reserva.getTarifaNoche(),
                reserva.getTotal(),
                reserva.getEstado(),
                reserva.getFechaRegistro()
        );
    }
}
