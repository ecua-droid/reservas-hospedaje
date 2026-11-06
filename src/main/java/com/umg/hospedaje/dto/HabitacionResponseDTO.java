package com.umg.hospedaje.dto;

import com.umg.hospedaje.entity.Habitacion;
import com.umg.hospedaje.entity.TipoHabitacion;

import java.math.BigDecimal;

/**
 * Información de una habitación que la API devuelve al frontend.
 * No expone la lista de reservas de la entidad.
 */
public record HabitacionResponseDTO(
        Long id,
        String numero,
        TipoHabitacion tipo,
        Integer capacidad,
        BigDecimal tarifaNoche,
        String descripcion,
        Boolean activa
) {

    /** Convierte la entidad en DTO. */
    public static HabitacionResponseDTO desde(Habitacion habitacion) {
        return new HabitacionResponseDTO(
                habitacion.getId(),
                habitacion.getNumero(),
                habitacion.getTipo(),
                habitacion.getCapacidad(),
                habitacion.getTarifaNoche(),
                habitacion.getDescripcion(),
                habitacion.getActiva()
        );
    }
}
