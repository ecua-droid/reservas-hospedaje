package com.umg.hospedaje.controller;

import com.umg.hospedaje.dto.DisponibilidadDTO;
import com.umg.hospedaje.dto.HabitacionRequestDTO;
import com.umg.hospedaje.dto.HabitacionResponseDTO;
import com.umg.hospedaje.dto.ReservaResponseDTO;
import com.umg.hospedaje.service.HabitacionService;
import com.umg.hospedaje.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Endpoints de habitaciones: /api/habitaciones
 * El controlador solo recibe la petición y delega en el servicio.
 */
@RestController
@RequestMapping("/api/habitaciones")
public class HabitacionController {

    private final HabitacionService habitacionService;
    private final ReservaService reservaService;

    public HabitacionController(HabitacionService habitacionService, ReservaService reservaService) {
        this.habitacionService = habitacionService;
        this.reservaService = reservaService;
    }

    /** GET /api/habitaciones  (opcional: ?activas=true) */
    @GetMapping
    public List<HabitacionResponseDTO> listar(@RequestParam(required = false) Boolean activas) {
        return habitacionService.listar(activas);
    }

    /** GET /api/habitaciones/{id} */
    @GetMapping("/{id}")
    public HabitacionResponseDTO buscarPorId(@PathVariable Long id) {
        return habitacionService.buscarPorId(id);
    }

    /** POST /api/habitaciones -> 201 Created */
    @PostMapping
    public ResponseEntity<HabitacionResponseDTO> registrar(@Valid @RequestBody HabitacionRequestDTO dto) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(habitacionService.registrar(dto));
    }

    /** PUT /api/habitaciones/{id} */
    @PutMapping("/{id}")
    public HabitacionResponseDTO actualizar(@PathVariable Long id,
                                            @Valid @RequestBody HabitacionRequestDTO dto) {
        return habitacionService.actualizar(id, dto);
    }

    /** DELETE /api/habitaciones/{id} -> eliminación lógica (desactiva) -> 204 No Content */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        habitacionService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    /** PATCH /api/habitaciones/{id}/activar */
    @PatchMapping("/{id}/activar")
    public HabitacionResponseDTO activar(@PathVariable Long id) {
        return habitacionService.activar(id);
    }

    /**
     * Consulta: habitaciones disponibles por periodo.
     * GET /api/habitaciones/disponibles?entrada=2026-11-20&salida=2026-11-23&huespedes=2
     */
    @GetMapping("/disponibles")
    public List<DisponibilidadDTO> disponibles(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate entrada,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate salida,
            @RequestParam(defaultValue = "1") Integer huespedes) {
        return habitacionService.buscarDisponibles(entrada, salida, huespedes);
    }

    /** Relación 1:N -> GET /api/habitaciones/{id}/reservas */
    @GetMapping("/{id}/reservas")
    public List<ReservaResponseDTO> reservasDeLaHabitacion(@PathVariable Long id) {
        return reservaService.listarPorHabitacion(id);
    }
}
