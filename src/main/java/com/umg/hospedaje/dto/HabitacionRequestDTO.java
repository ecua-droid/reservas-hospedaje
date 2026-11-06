package com.umg.hospedaje.dto;

import com.umg.hospedaje.entity.TipoHabitacion;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Datos que el cliente envía para registrar o modificar una habitación.
 * No incluye "id" ni "activa": esos los controla el sistema.
 */
public record HabitacionRequestDTO(

        @NotBlank(message = "El número de habitación es obligatorio")
        @Pattern(regexp = "^[A-Za-z0-9-]{1,10}$",
                message = "El número admite de 1 a 10 letras, dígitos o guiones")
        String numero,

        @NotNull(message = "El tipo de habitación es obligatorio")
        TipoHabitacion tipo,

        @NotNull(message = "La capacidad es obligatoria")
        @Min(value = 1, message = "La capacidad mínima es 1 huésped")
        @Max(value = 10, message = "La capacidad máxima es 10 huéspedes")
        Integer capacidad,

        @NotNull(message = "La tarifa por noche es obligatoria")
        @DecimalMin(value = "0.01", message = "La tarifa debe ser mayor que cero")
        @Digits(integer = 8, fraction = 2, message = "La tarifa admite como máximo 2 decimales")
        BigDecimal tarifaNoche,

        @Size(max = 200, message = "La descripción admite como máximo 200 caracteres")
        String descripcion
) {
}
