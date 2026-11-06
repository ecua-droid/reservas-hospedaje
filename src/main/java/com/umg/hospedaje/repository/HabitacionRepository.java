package com.umg.hospedaje.repository;

import com.umg.hospedaje.entity.EstadoReserva;
import com.umg.hospedaje.entity.Habitacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * Acceso a la tabla "habitacion".
 * JpaRepository ya incluye save, findById, findAll, count, existsById...
 */
public interface HabitacionRepository extends JpaRepository<Habitacion, Long> {

    boolean existsByNumeroIgnoreCase(String numero);

    boolean existsByNumeroIgnoreCaseAndIdNot(String numero, Long id);

    List<Habitacion> findAllByOrderByNumeroAsc();

    List<Habitacion> findByActivaTrueOrderByNumeroAsc();

    long countByActivaTrue();

    /**
     * Habitaciones activas, con capacidad suficiente y SIN reservas activas
     * que se traslapen con el periodo [entrada, salida).
     */
    @Query("""
            SELECT h FROM Habitacion h
            WHERE h.activa = true
              AND h.capacidad >= :huespedes
              AND NOT EXISTS (
                    SELECT r.id FROM Reserva r
                    WHERE r.habitacion = h
                      AND r.estado = :estado
                      AND r.fechaEntrada < :salida
                      AND r.fechaSalida > :entrada
              )
            ORDER BY h.numero
            """)
    List<Habitacion> buscarDisponibles(@Param("entrada") LocalDate entrada,
                                       @Param("salida") LocalDate salida,
                                       @Param("huespedes") int huespedes,
                                       @Param("estado") EstadoReserva estado);
}
