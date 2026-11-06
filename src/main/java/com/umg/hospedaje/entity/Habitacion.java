package com.umg.hospedaje.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad Habitación (tabla "habitacion").
 * Relación: una habitación puede tener muchas reservas (1 : N).
 */
@Entity
@Table(
        name = "habitacion",
        uniqueConstraints = @UniqueConstraint(name = "uk_habitacion_numero", columnNames = "numero")
)
public class Habitacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Número visible de la habitación, por ejemplo "101". No se puede repetir. */
    @Column(nullable = false, length = 10)
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoHabitacion tipo;

    /** Cantidad máxima de huéspedes. */
    @Column(nullable = false)
    private Integer capacidad;

    /** Precio por noche en quetzales. */
    @Column(name = "tarifa_noche", nullable = false, precision = 10, scale = 2)
    private BigDecimal tarifaNoche;

    @Column(length = 200)
    private String descripcion;

    /** Eliminación lógica: una habitación desactivada no acepta reservas nuevas. */
    @Column(nullable = false)
    private Boolean activa = true;

    /** Lado inverso de la relación 1:N (la llave foránea está en Reserva). */
    @OneToMany(mappedBy = "habitacion")
    private List<Reserva> reservas = new ArrayList<>();

    public Habitacion() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public TipoHabitacion getTipo() {
        return tipo;
    }

    public void setTipo(TipoHabitacion tipo) {
        this.tipo = tipo;
    }

    public Integer getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(Integer capacidad) {
        this.capacidad = capacidad;
    }

    public BigDecimal getTarifaNoche() {
        return tarifaNoche;
    }

    public void setTarifaNoche(BigDecimal tarifaNoche) {
        this.tarifaNoche = tarifaNoche;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Boolean getActiva() {
        return activa;
    }

    public void setActiva(Boolean activa) {
        this.activa = activa;
    }

    public List<Reserva> getReservas() {
        return reservas;
    }
}
