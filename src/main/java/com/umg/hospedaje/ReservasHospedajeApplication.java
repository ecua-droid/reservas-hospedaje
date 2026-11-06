package com.umg.hospedaje;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de inicio del sistema de reservas de habitaciones.
 * Al ejecutarse levanta el servidor en http://localhost:8080
 */
@SpringBootApplication
public class ReservasHospedajeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReservasHospedajeApplication.class, args);
    }
}
