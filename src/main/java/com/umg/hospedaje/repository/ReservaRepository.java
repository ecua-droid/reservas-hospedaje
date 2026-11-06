package com.umg.hospedaje.repository;

import com.umg.hospedaje.entity.EstadoReserva;
import com.umg.hospedaje.entity.Reserva;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Acceso a la tabla "reserva".
 * @EntityGraph("habitacion") trae la habitación en la misma consulta (evita consultas extra).
 */
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    @EntityGraph(attributePaths = "habitacion")
    List<Reserva> findAllByOrderByFechaEntradaDesc();

    /** Consulta: reservas activas. */
    @EntityGraph(attributePaths = "habitacion")
    List<Reserva> findByEstadoOrderByFechaEntradaAsc(EstadoReserva estado);

    /** Relación: reservas de una habitación. */
    @EntityGraph(attributePaths = "habitacion")
    List<Reserva> findByHabitacionIdOrderByFechaEntradaDesc(Long habitacionId);

    /** Usada para revisar traslapes (regla 2). */
    List<Reserva> findByHabitacionIdAndEstado(Long habitacionId, EstadoReserva estado);

    /** Consulta: reservas por huésped (parte del nombre o documento exacto). */
    @Query("""
            SELECT r FROM Reserva r JOIN FETCH r.habitacion
            WHERE LOWER(r.nombreHuesped) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(r.documentoHuesped) = LOWER(:texto)
            ORDER BY r.fechaEntrada DESC
            """)
    List<Reserva> buscarPorHuesped(@Param("texto") String texto);

    long countByEstado(EstadoReserva estado);

    long countByHabitacionIdAndEstado(Long habitacionId, EstadoReserva estado);

    boolean existsByHabitacionIdAndEstadoAndCantidadHuespedesGreaterThan(Long habitacionId,
                                                                         EstadoReserva estado,
                                                                         Integer cantidad);

    /** Suma de los totales de las reservas en un estado (null si no hay ninguna). */
    @Query("SELECT SUM(r.total) FROM Reserva r WHERE r.estado = :estado")
    BigDecimal sumarTotalPorEstado(@Param("estado") EstadoReserva estado);

    /** Habitaciones ocupadas en una fecha: reserva activa con entrada <= fecha < salida. */
    @Query("""
            SELECT COUNT(DISTINCT r.habitacion.id) FROM Reserva r
            WHERE r.estado = :estado
              AND r.fechaEntrada <= :fecha
              AND r.fechaSalida > :fecha
            """)
    long contarHabitacionesOcupadas(@Param("estado") EstadoReserva estado,
                                    @Param("fecha") LocalDate fecha);
}
