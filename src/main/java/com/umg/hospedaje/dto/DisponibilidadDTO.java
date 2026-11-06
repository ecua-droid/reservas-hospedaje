package com.umg.hospedaje.dto;

import com.umg.hospedaje.entity.TipoHabitacion;

import java.math.BigDecimal;

/**
 * Resultado de la consulta "habitaciones disponibles por periodo":
 * una habitación libre y lo que costaría quedarse en ella esas noches.
 */
public record DisponibilidadDTO(
        Long habitacionId,
        String numero,
        TipoHabitacion tipo,
        Integer capacidad,
        BigDecimal tarifaNoche,
        String descripcion,
        int noches,
        BigDecimal totalEstimado
) {
}
