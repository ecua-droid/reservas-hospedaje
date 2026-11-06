package com.umg.hospedaje.controller;

import com.umg.hospedaje.dto.CotizacionDTO;
import com.umg.hospedaje.dto.ReservaRequestDTO;
import com.umg.hospedaje.dto.ReservaResponseDTO;
import com.umg.hospedaje.dto.ResumenReservasDTO;
import com.umg.hospedaje.dto.TotalReservaDTO;
import com.umg.hospedaje.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Endpoints de reservas: /api/reservas
 */
@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    /** GET /api/reservas */
    @GetMapping
    public List<ReservaResponseDTO> listar() {
        return reservaService.listar();
    }

    /** GET /api/reservas/{id} */
    @GetMapping("/{id}")
    public ReservaResponseDTO buscarPorId(@PathVariable Long id) {
        return reservaService.buscarPorId(id);
    }

    /** POST /api/reservas -> 201 Created (operación de negocio: valida reglas y calcula el total) */
    @PostMapping
    public ResponseEntity<ReservaResponseDTO> registrar(@Valid @RequestBody ReservaRequestDTO dto) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(reservaService.registrar(dto));
    }

    /** PUT /api/reservas/{id} */
    @PutMapping("/{id}")
    public ReservaResponseDTO actualizar(@PathVariable Long id, @Valid @RequestBody ReservaRequestDTO dto) {
        return reservaService.actualizar(id, dto);
    }

    /** PATCH /api/reservas/{id}/cancelar */
    @PatchMapping("/{id}/cancelar")
    public ReservaResponseDTO cancelar(@PathVariable Long id) {
        return reservaService.cancelar(id);
    }

    /** PATCH /api/reservas/{id}/finalizar  (check-out) */
    @PatchMapping("/{id}/finalizar")
    public ReservaResponseDTO finalizar(@PathVariable Long id) {
        return reservaService.finalizar(id);
    }

    /** Consulta: GET /api/reservas/activas */
    @GetMapping("/activas")
    public List<ReservaResponseDTO> activas() {
        return reservaService.listarActivas();
    }

    /** Consulta: GET /api/reservas/huesped?texto=Ana  (nombre o documento) */
    @GetMapping("/huesped")
    public List<ReservaResponseDTO> porHuesped(@RequestParam String texto) {
        return reservaService.buscarPorHuesped(texto);
    }

    /** Indicador: GET /api/reservas/{id}/total */
    @GetMapping("/{id}/total")
    public TotalReservaDTO totalEstimado(@PathVariable Long id) {
        return reservaService.calcularTotal(id);
    }

    /**
     * Total estimado antes de reservar:
     * GET /api/reservas/cotizar?habitacionId=3&entrada=2026-11-20&salida=2026-11-23
     * (reservaId es opcional: se usa al modificar una reserva)
     */
    @GetMapping("/cotizar")
    public CotizacionDTO cotizar(
            @RequestParam Long habitacionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate entrada,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate salida,
            @RequestParam(required = false) Long reservaId) {
        return reservaService.cotizar(habitacionId, entrada, salida, reservaId);
    }

    /** Indicador general: GET /api/reservas/resumen */
    @GetMapping("/resumen")
    public ResumenReservasDTO resumen() {
        return reservaService.obtenerResumen();
    }
}
