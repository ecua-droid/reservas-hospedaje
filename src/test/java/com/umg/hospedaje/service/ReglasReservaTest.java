package com.umg.hospedaje.service;

import com.umg.hospedaje.exception.ReglaNegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas unitarias de las 3 reglas principales (no necesitan base de datos).
 * Ejecutar con:  .\mvnw.cmd test
 */
class ReglasReservaTest {

    private static final LocalDate DIA_10 = LocalDate.of(2026, 11, 10);

    // ------------------------------------------------ Regla 1: salida posterior a entrada

    @Test
    void salidaPosteriorALaEntradaEsValida() {
        assertThatCode(() -> ReglasReserva.validarPeriodo(DIA_10, DIA_10.plusDays(2)))
                .doesNotThrowAnyException();
    }

    @Test
    void salidaIgualALaEntradaSeRechaza() {
        assertThatThrownBy(() -> ReglasReserva.validarPeriodo(DIA_10, DIA_10))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("posterior");
    }

    @Test
    void salidaAnteriorALaEntradaSeRechaza() {
        assertThatThrownBy(() -> ReglasReserva.validarPeriodo(DIA_10, DIA_10.minusDays(1)))
                .isInstanceOf(ReglaNegocioException.class);
    }

    // ------------------------------------------------ Regla 2: traslapes

    @Test
    void periodosQueSeCruzanSeTraslapan() {
        // del 10 al 13  vs  del 12 al 15
        assertThat(ReglasReserva.seTraslapan(DIA_10, DIA_10.plusDays(3), DIA_10.plusDays(2), DIA_10.plusDays(5)))
                .isTrue();
    }

    @Test
    void periodoDentroDeOtroSeTraslapa() {
        // del 10 al 20  vs  del 12 al 14
        assertThat(ReglasReserva.seTraslapan(DIA_10, DIA_10.plusDays(10), DIA_10.plusDays(2), DIA_10.plusDays(4)))
                .isTrue();
    }

    @Test
    void salirElMismoDiaQueOtroEntraNoEsTraslape() {
        // del 10 al 12  vs  del 12 al 14 -> el día 12 uno sale y el otro entra
        assertThat(ReglasReserva.seTraslapan(DIA_10, DIA_10.plusDays(2), DIA_10.plusDays(2), DIA_10.plusDays(4)))
                .isFalse();
    }

    @Test
    void periodosSeparadosNoSeTraslapan() {
        // del 10 al 12  vs  del 15 al 18
        assertThat(ReglasReserva.seTraslapan(DIA_10, DIA_10.plusDays(2), DIA_10.plusDays(5), DIA_10.plusDays(8)))
                .isFalse();
    }

    // ------------------------------------------------ Regla 3: total

    @Test
    void calculaLasNochesEntreFechas() {
        assertThat(ReglasReserva.calcularNoches(DIA_10, DIA_10.plusDays(3))).isEqualTo(3);
    }

    @Test
    void totalEsNochesPorTarifa() {
        assertThat(ReglasReserva.calcularTotal(3, new BigDecimal("350.00")))
                .isEqualByComparingTo("1050.00");
    }
}
