package com.umg.hospedaje.service;

import com.umg.hospedaje.dto.DisponibilidadDTO;
import com.umg.hospedaje.dto.HabitacionRequestDTO;
import com.umg.hospedaje.dto.HabitacionResponseDTO;
import com.umg.hospedaje.entity.EstadoReserva;
import com.umg.hospedaje.entity.Habitacion;
import com.umg.hospedaje.exception.RecursoNoEncontradoException;
import com.umg.hospedaje.exception.RegistroDuplicadoException;
import com.umg.hospedaje.exception.ReglaNegocioException;
import com.umg.hospedaje.exception.SolicitudInvalidaException;
import com.umg.hospedaje.repository.HabitacionRepository;
import com.umg.hospedaje.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Lógica de negocio de las habitaciones.
 * Por defecto los métodos son de solo lectura; los que modifican datos usan @Transactional.
 */
@Service
@Transactional(readOnly = true)
public class HabitacionService {

    private final HabitacionRepository habitacionRepository;
    private final ReservaRepository reservaRepository;

    public HabitacionService(HabitacionRepository habitacionRepository,
                             ReservaRepository reservaRepository) {
        this.habitacionRepository = habitacionRepository;
        this.reservaRepository = reservaRepository;
    }

    /** Lista todas las habitaciones o, si soloActivas = true, únicamente las activas. */
    public List<HabitacionResponseDTO> listar(Boolean soloActivas) {
        List<Habitacion> habitaciones = Boolean.TRUE.equals(soloActivas)
                ? habitacionRepository.findByActivaTrueOrderByNumeroAsc()
                : habitacionRepository.findAllByOrderByNumeroAsc();

        return habitaciones.stream()
                .map(HabitacionResponseDTO::desde)
                .toList();
    }

    public HabitacionResponseDTO buscarPorId(Long id) {
        return HabitacionResponseDTO.desde(obtenerHabitacion(id));
    }

    @Transactional
    public HabitacionResponseDTO registrar(HabitacionRequestDTO dto) {
        String numero = normalizarNumero(dto.numero());
        if (habitacionRepository.existsByNumeroIgnoreCase(numero)) {
            throw new RegistroDuplicadoException("Ya existe una habitación con el número " + numero);
        }

        Habitacion habitacion = new Habitacion();
        copiarDatos(dto, habitacion);
        habitacion.setActiva(true);

        return HabitacionResponseDTO.desde(habitacionRepository.save(habitacion));
    }

    @Transactional
    public HabitacionResponseDTO actualizar(Long id, HabitacionRequestDTO dto) {
        Habitacion habitacion = obtenerHabitacion(id);

        String numero = normalizarNumero(dto.numero());
        if (habitacionRepository.existsByNumeroIgnoreCaseAndIdNot(numero, id)) {
            throw new RegistroDuplicadoException("Ya existe otra habitación con el número " + numero);
        }

        // No se puede reducir la capacidad por debajo de una reserva activa ya registrada
        boolean reservaExcedeCapacidad = reservaRepository
                .existsByHabitacionIdAndEstadoAndCantidadHuespedesGreaterThan(id, EstadoReserva.ACTIVA, dto.capacidad());
        if (reservaExcedeCapacidad) {
            throw new ReglaNegocioException("No se puede reducir la capacidad a " + dto.capacidad()
                    + ": la habitación tiene reservas activas con más huéspedes");
        }

        copiarDatos(dto, habitacion);
        return HabitacionResponseDTO.desde(habitacionRepository.save(habitacion));
    }

    /**
     * Eliminación lógica: la habitación se desactiva en lugar de borrarse,
     * para conservar el historial de reservas (llave foránea).
     */
    @Transactional
    public void desactivar(Long id) {
        Habitacion habitacion = obtenerHabitacion(id);

        if (!habitacion.getActiva()) {
            throw new ReglaNegocioException("La habitación " + habitacion.getNumero() + " ya está desactivada");
        }

        long reservasActivas = reservaRepository.countByHabitacionIdAndEstado(id, EstadoReserva.ACTIVA);
        if (reservasActivas > 0) {
            throw new ReglaNegocioException("No se puede desactivar la habitación " + habitacion.getNumero()
                    + " porque tiene " + reservasActivas + " reserva(s) activa(s). Cancélelas o finalícelas primero.");
        }

        habitacion.setActiva(false);
        habitacionRepository.save(habitacion);
    }

    @Transactional
    public HabitacionResponseDTO activar(Long id) {
        Habitacion habitacion = obtenerHabitacion(id);

        if (habitacion.getActiva()) {
            throw new ReglaNegocioException("La habitación " + habitacion.getNumero() + " ya está activa");
        }

        habitacion.setActiva(true);
        return HabitacionResponseDTO.desde(habitacionRepository.save(habitacion));
    }

    /**
     * Consulta: habitaciones disponibles en un periodo, con el total estimado
     * que costaría cada una en esas fechas.
     */
    public List<DisponibilidadDTO> buscarDisponibles(LocalDate entrada, LocalDate salida, Integer huespedes) {
        ReglasReserva.validarPeriodo(entrada, salida);

        int cantidad = (huespedes == null) ? 1 : huespedes;
        if (cantidad < 1) {
            throw new SolicitudInvalidaException("La cantidad de huéspedes debe ser al menos 1");
        }

        int noches = ReglasReserva.calcularNoches(entrada, salida);

        return habitacionRepository.buscarDisponibles(entrada, salida, cantidad, EstadoReserva.ACTIVA)
                .stream()
                .map(h -> new DisponibilidadDTO(
                        h.getId(),
                        h.getNumero(),
                        h.getTipo(),
                        h.getCapacidad(),
                        h.getTarifaNoche(),
                        h.getDescripcion(),
                        noches,
                        ReglasReserva.calcularTotal(noches, h.getTarifaNoche())
                ))
                .toList();
    }

    // ----------------------------------------------------------------- privados

    private Habitacion obtenerHabitacion(Long id) {
        return habitacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la habitación con ID " + id));
    }

    private void copiarDatos(HabitacionRequestDTO dto, Habitacion habitacion) {
        habitacion.setNumero(normalizarNumero(dto.numero()));
        habitacion.setTipo(dto.tipo());
        habitacion.setCapacidad(dto.capacidad());
        habitacion.setTarifaNoche(dto.tarifaNoche().setScale(2, RoundingMode.HALF_UP));
        habitacion.setDescripcion(textoONulo(dto.descripcion()));
    }

    private String normalizarNumero(String numero) {
        return numero.trim().toUpperCase();
    }

    private String textoONulo(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
