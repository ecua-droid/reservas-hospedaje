package com.umg.hospedaje.service;

import com.umg.hospedaje.exception.ReglaNegocioException;
import com.umg.hospedaje.exception.SolicitudInvalidaException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Reglas principales del negocio de reservas, reunidas en un solo lugar:
 *
 *  1. La fecha de salida debe ser posterior a la fecha de entrada.
 *  2. No se permiten reservas activas que se traslapen en la misma habitación.
 *  3. El total se calcula con las noches y la tarifa de la habitación.
 *
 * Son métodos estáticos y no dependen de la base de datos,
 * por eso se pueden probar con pruebas unitarias (ReglasReservaTest).
 */
public final class ReglasReserva {

    private ReglasReserva() {
    }

    /** Regla 1: la fecha de salida debe ser posterior a la de entrada. */
    public static void validarPeriodo(LocalDate entrada, LocalDate salida) {
        if (entrada == null || salida == null) {
            throw new SolicitudInvalidaException("Las fechas de entrada y salida son obligatorias");
        }
        if (!salida.isAfter(entrada)) {
            throw new ReglaNegocioException("La fecha de salida debe ser posterior a la fecha de entrada");
        }
    }

    /**
     * Regla 2: indica si dos periodos se traslapan.
     *
     * Cada periodo va desde la entrada hasta la salida, sin incluir el día de salida:
     * así, si un huésped sale el día 12, otro puede entrar ese mismo día 12.
     *
     * Hay traslape cuando el periodo A empieza antes de que termine B
     * y además termina después de que empieza B.
     */
    public static boolean seTraslapan(LocalDate entradaA, LocalDate salidaA,
                                      LocalDate entradaB, LocalDate salidaB) {
        return entradaA.isBefore(salidaB) && salidaA.isAfter(entradaB);
    }

    /** Noches = días entre la entrada y la salida. */
    public static int calcularNoches(LocalDate entrada, LocalDate salida) {
        return (int) ChronoUnit.DAYS.between(entrada, salida);
    }

    /** Regla 3: total = noches x tarifa por noche (2 decimales). */
    public static BigDecimal calcularTotal(int noches, BigDecimal tarifaNoche) {
        return tarifaNoche
                .multiply(BigDecimal.valueOf(noches))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
