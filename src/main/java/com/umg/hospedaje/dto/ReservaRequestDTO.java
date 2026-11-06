package com.umg.hospedaje.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Datos que el cliente envía para registrar o modificar una reserva.
 * Noches, tarifa, total y estado NO se reciben: los calcula el sistema.
 */
public record ReservaRequestDTO(

        @NotNull(message = "Debe indicar la habitación")
        @Positive(message = "El ID de la habitación debe ser mayor que cero")
        Long habitacionId,

        @NotBlank(message = "El nombre del huésped es obligatorio")
        @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
        String nombreHuesped,

        @NotBlank(message = "El documento del huésped es obligatorio")
        @Pattern(regexp = "^[A-Za-z0-9-]{5,20}$",
                message = "El documento debe tener de 5 a 20 letras, dígitos o guiones")
        String documentoHuesped,

        /* Opcional: vacío o de 8 a 15 caracteres (dígitos, espacios, + o -) */
        @Pattern(regexp = "^$|^[0-9 +-]{8,15}$",
                message = "El teléfono debe tener de 8 a 15 dígitos")
        String telefonoHuesped,

        @NotNull(message = "La cantidad de huéspedes es obligatoria")
        @Min(value = 1, message = "Debe haber al menos 1 huésped")
        @Max(value = 10, message = "No se permiten más de 10 huéspedes")
        Integer cantidadHuespedes,

        @NotNull(message = "La fecha de entrada es obligatoria")
        LocalDate fechaEntrada,

        @NotNull(message = "La fecha de salida es obligatoria")
        LocalDate fechaSalida
) {
}
