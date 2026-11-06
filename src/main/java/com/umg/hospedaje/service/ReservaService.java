package com.umg.hospedaje.service;

import com.umg.hospedaje.dto.CotizacionDTO;
import com.umg.hospedaje.dto.ReservaRequestDTO;
import com.umg.hospedaje.dto.ReservaResponseDTO;
import com.umg.hospedaje.dto.ResumenReservasDTO;
import com.umg.hospedaje.dto.TotalReservaDTO;
import com.umg.hospedaje.entity.EstadoReserva;
import com.umg.hospedaje.entity.Habitacion;
import com.umg.hospedaje.entity.Reserva;
import com.umg.hospedaje.exception.RecursoNoEncontradoException;
import com.umg.hospedaje.exception.ReglaNegocioException;
import com.umg.hospedaje.exception.ReservaTraslapadaException;
import com.umg.hospedaje.exception.SolicitudInvalidaException;
import com.umg.hospedaje.repository.HabitacionRepository;
import com.umg.hospedaje.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Lógica de negocio de las reservas: aquí se aplican las 3 reglas principales
 * antes de guardar cualquier dato.
 */
@Service
@Transactional(readOnly = true)
public class ReservaService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ReservaRepository reservaRepository;
    private final HabitacionRepository habitacionRepository;

    public ReservaService(ReservaRepository reservaRepository,
                          HabitacionRepository habitacionRepository) {
        this.reservaRepository = reservaRepository;
        this.habitacionRepository = habitacionRepository;
    }

    // ================================================================ consultas

    public List<ReservaResponseDTO> listar() {
        return convertir(reservaRepository.findAllByOrderByFechaEntradaDesc());
    }

    public ReservaResponseDTO buscarPorId(Long id) {
        return ReservaResponseDTO.desde(obtenerReserva(id));
    }

    /** Consulta: reservas activas. */
    public List<ReservaResponseDTO> listarActivas() {
        return convertir(reservaRepository.findByEstadoOrderByFechaEntradaAsc(EstadoReserva.ACTIVA));
    }

    /** Consulta: reservas por huésped (parte del nombre o documento). */
    public List<ReservaResponseDTO> buscarPorHuesped(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new SolicitudInvalidaException("Escriba el nombre o el documento del huésped que desea buscar");
        }
        return convertir(reservaRepository.buscarPorHuesped(texto.trim()));
    }

    /** Relación: todas las reservas de una habitación. */
    public List<ReservaResponseDTO> listarPorHabitacion(Long habitacionId) {
        if (!habitacionRepository.existsById(habitacionId)) {
            throw new RecursoNoEncontradoException("No existe la habitación con ID " + habitacionId);
        }
        return convertir(reservaRepository.findByHabitacionIdOrderByFechaEntradaDesc(habitacionId));
    }

    // ======================================================= operación de negocio

    /** Registra una reserva aplicando todas las reglas. */
    @Transactional
    public ReservaResponseDTO registrar(ReservaRequestDTO dto) {
        Habitacion habitacion = obtenerHabitacion(dto.habitacionId());

        validarHabitacionActiva(habitacion);
        ReglasReserva.validarPeriodo(dto.fechaEntrada(), dto.fechaSalida());          // Regla 1
        validarEntradaNoPasada(dto.fechaEntrada());
        validarCapacidad(habitacion, dto.cantidadHuespedes());
        validarSinTraslape(habitacion, dto.fechaEntrada(), dto.fechaSalida(), null);  // Regla 2

        Reserva reserva = new Reserva();
        reserva.setEstado(EstadoReserva.ACTIVA);
        copiarDatos(dto, habitacion, reserva);                                         // Regla 3

        return ReservaResponseDTO.desde(reservaRepository.save(reserva));
    }

    /** Modifica una reserva ACTIVA (fechas, habitación o datos del huésped) y recalcula el total. */
    @Transactional
    public ReservaResponseDTO actualizar(Long id, ReservaRequestDTO dto) {
        Reserva reserva = obtenerReserva(id);
        validarQueEsteActiva(reserva, "modificar");

        Habitacion habitacion = obtenerHabitacion(dto.habitacionId());
        validarHabitacionActiva(habitacion);
        ReglasReserva.validarPeriodo(dto.fechaEntrada(), dto.fechaSalida());

        // Una estadía que ya comenzó puede conservar su fecha de entrada pasada
        if (!dto.fechaEntrada().equals(reserva.getFechaEntrada())) {
            validarEntradaNoPasada(dto.fechaEntrada());
        }

        validarCapacidad(habitacion, dto.cantidadHuespedes());
        // Se excluye la propia reserva para que no "choque" consigo misma
        validarSinTraslape(habitacion, dto.fechaEntrada(), dto.fechaSalida(), reserva.getId());

        copiarDatos(dto, habitacion, reserva);
        return ReservaResponseDTO.desde(reservaRepository.save(reserva));
    }

    /** Cancela una reserva activa: libera las fechas de la habitación. */
    @Transactional
    public ReservaResponseDTO cancelar(Long id) {
        Reserva reserva = obtenerReserva(id);
        validarQueEsteActiva(reserva, "cancelar");

        reserva.setEstado(EstadoReserva.CANCELADA);
        return ReservaResponseDTO.desde(reservaRepository.save(reserva));
    }

    /** Finaliza (check-out) una reserva activa cuya estadía ya comenzó. */
    @Transactional
    public ReservaResponseDTO finalizar(Long id) {
        Reserva reserva = obtenerReserva(id);
        validarQueEsteActiva(reserva, "finalizar");

        if (reserva.getFechaEntrada().isAfter(LocalDate.now())) {
            throw new ReglaNegocioException("No se puede finalizar la reserva #" + id
                    + " porque la estadía aún no comienza (entrada: " + formatear(reserva.getFechaEntrada())
                    + "). Si el huésped no vendrá, cancélela.");
        }

        reserva.setEstado(EstadoReserva.FINALIZADA);
        return ReservaResponseDTO.desde(reservaRepository.save(reserva));
    }

    // ================================================================ indicadores

    /** Indicador: total estimado de una reserva registrada (noches x tarifa). */
    public TotalReservaDTO calcularTotal(Long id) {
        Reserva reserva = obtenerReserva(id);

        int noches = ReglasReserva.calcularNoches(reserva.getFechaEntrada(), reserva.getFechaSalida());
        BigDecimal total = ReglasReserva.calcularTotal(noches, reserva.getTarifaNoche());

        return new TotalReservaDTO(
                reserva.getId(),
                reserva.getHabitacion().getNumero(),
                reserva.getNombreHuesped(),
                reserva.getFechaEntrada(),
                reserva.getFechaSalida(),
                noches,
                reserva.getTarifaNoche(),
                total,
                reserva.getEstado(),
                describirCalculo(noches, reserva.getTarifaNoche(), total)
        );
    }

    /**
     * Total estimado ANTES de reservar y si la habitación está libre.
     * reservaIdExcluir se usa al modificar, para no contar la propia reserva.
     */
    public CotizacionDTO cotizar(Long habitacionId, LocalDate entrada, LocalDate salida, Long reservaIdExcluir) {
        Habitacion habitacion = obtenerHabitacion(habitacionId);
        ReglasReserva.validarPeriodo(entrada, salida);

        int noches = ReglasReserva.calcularNoches(entrada, salida);
        BigDecimal total = ReglasReserva.calcularTotal(noches, habitacion.getTarifaNoche());
        Optional<Reserva> cruce = buscarTraslape(habitacion, entrada, salida, reservaIdExcluir);

        boolean disponible = habitacion.getActiva() && cruce.isEmpty();
        String mensaje;
        if (!habitacion.getActiva()) {
            mensaje = "La habitación " + habitacion.getNumero() + " está desactivada";
        } else if (cruce.isPresent()) {
            mensaje = describirCruce(cruce.get());
        } else {
            mensaje = "Disponible: " + describirCalculo(noches, habitacion.getTarifaNoche(), total);
        }

        return new CotizacionDTO(habitacion.getId(), habitacion.getNumero(), entrada, salida,
                noches, habitacion.getTarifaNoche(), total, disponible, mensaje);
    }

    /** Indicador general: ocupación de hoy, conteo por estado e ingreso estimado. */
    public ResumenReservasDTO obtenerResumen() {
        LocalDate hoy = LocalDate.now();

        long totalHabitaciones = habitacionRepository.count();
        long habitacionesActivas = habitacionRepository.countByActivaTrue();
        long ocupadasHoy = reservaRepository.contarHabitacionesOcupadas(EstadoReserva.ACTIVA, hoy);

        BigDecimal porcentajeOcupacion = BigDecimal.ZERO.setScale(2);
        if (habitacionesActivas > 0) {
            porcentajeOcupacion = BigDecimal.valueOf(ocupadasHoy * 100)
                    .divide(BigDecimal.valueOf(habitacionesActivas), 2, RoundingMode.HALF_UP);
        }

        BigDecimal ingresoEstimado = reservaRepository.sumarTotalPorEstado(EstadoReserva.ACTIVA);
        if (ingresoEstimado == null) {
            ingresoEstimado = BigDecimal.ZERO;
        }

        return new ResumenReservasDTO(
                hoy,
                totalHabitaciones,
                habitacionesActivas,
                ocupadasHoy,
                porcentajeOcupacion,
                reservaRepository.countByEstado(EstadoReserva.ACTIVA),
                reservaRepository.countByEstado(EstadoReserva.CANCELADA),
                reservaRepository.countByEstado(EstadoReserva.FINALIZADA),
                ingresoEstimado.setScale(2, RoundingMode.HALF_UP)
        );
    }

    // ================================================================ validaciones

    /** Regla 2: lanza un error si hay una reserva activa que se cruce con las fechas. */
    private void validarSinTraslape(Habitacion habitacion, LocalDate entrada, LocalDate salida,
                                    Long reservaIdExcluir) {
        Optional<Reserva> cruce = buscarTraslape(habitacion, entrada, salida, reservaIdExcluir);
        if (cruce.isPresent()) {
            throw new ReservaTraslapadaException(describirCruce(cruce.get())
                    + ". Elija otras fechas u otra habitación.");
        }
    }

    /** Recorre las reservas activas de la habitación y devuelve la primera que se traslapa. */
    private Optional<Reserva> buscarTraslape(Habitacion habitacion, LocalDate entrada, LocalDate salida,
                                             Long reservaIdExcluir) {
        List<Reserva> activas = reservaRepository.findByHabitacionIdAndEstado(habitacion.getId(), EstadoReserva.ACTIVA);

        for (Reserva existente : activas) {
            boolean esLaMismaReserva = existente.getId().equals(reservaIdExcluir);
            if (!esLaMismaReserva && ReglasReserva.seTraslapan(entrada, salida,
                    existente.getFechaEntrada(), existente.getFechaSalida())) {
                return Optional.of(existente);
            }
        }
        return Optional.empty();
    }

    private void validarHabitacionActiva(Habitacion habitacion) {
        if (!Boolean.TRUE.equals(habitacion.getActiva())) {
            throw new ReglaNegocioException("La habitación " + habitacion.getNumero()
                    + " está desactivada y no admite reservas");
        }
    }

    private void validarEntradaNoPasada(LocalDate entrada) {
        LocalDate hoy = LocalDate.now();
        if (entrada.isBefore(hoy)) {
            throw new ReglaNegocioException("La fecha de entrada no puede ser anterior a hoy (" + formatear(hoy) + ")");
        }
    }

    private void validarCapacidad(Habitacion habitacion, int cantidadHuespedes) {
        if (cantidadHuespedes > habitacion.getCapacidad()) {
            throw new ReglaNegocioException("La habitación " + habitacion.getNumero() + " admite máximo "
                    + habitacion.getCapacidad() + " huésped(es) y la reserva indica " + cantidadHuespedes);
        }
    }

    private void validarQueEsteActiva(Reserva reserva, String accion) {
        if (reserva.getEstado() != EstadoReserva.ACTIVA) {
            throw new ReglaNegocioException("No se puede " + accion + " la reserva #" + reserva.getId()
                    + " porque está " + reserva.getEstado().name().toLowerCase());
        }
    }

    // ================================================================ auxiliares

    private void copiarDatos(ReservaRequestDTO dto, Habitacion habitacion, Reserva reserva) {
        reserva.setHabitacion(habitacion);
        reserva.setNombreHuesped(dto.nombreHuesped().trim());
        reserva.setDocumentoHuesped(dto.documentoHuesped().trim().toUpperCase());
        reserva.setTelefonoHuesped(textoONulo(dto.telefonoHuesped()));
        reserva.setCantidadHuespedes(dto.cantidadHuespedes());
        reserva.setFechaEntrada(dto.fechaEntrada());
        reserva.setFechaSalida(dto.fechaSalida());

        // Regla 3: total = noches x tarifa actual de la habitación
        int noches = ReglasReserva.calcularNoches(dto.fechaEntrada(), dto.fechaSalida());
        reserva.setNoches(noches);
        reserva.setTarifaNoche(habitacion.getTarifaNoche());
        reserva.setTotal(ReglasReserva.calcularTotal(noches, habitacion.getTarifaNoche()));
    }

    private Reserva obtenerReserva(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la reserva con ID " + id));
    }

    private Habitacion obtenerHabitacion(Long id) {
        return habitacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la habitación con ID " + id));
    }

    private List<ReservaResponseDTO> convertir(List<Reserva> reservas) {
        return reservas.stream()
                .map(ReservaResponseDTO::desde)
                .toList();
    }

    private String describirCruce(Reserva cruce) {
        return "La habitación " + cruce.getHabitacion().getNumero() + " ya tiene la reserva activa #"
                + cruce.getId() + " del " + formatear(cruce.getFechaEntrada())
                + " al " + formatear(cruce.getFechaSalida());
    }

    private String describirCalculo(int noches, BigDecimal tarifa, BigDecimal total) {
        return noches + " noche(s) x Q" + tarifa + " = Q" + total;
    }

    private String formatear(LocalDate fecha) {
        return fecha.format(FORMATO_FECHA);
    }

    private String textoONulo(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
