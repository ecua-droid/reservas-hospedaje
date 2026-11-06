/* =========================================================
 * Hospedaje La Ceiba - lógica del frontend
 * Consume la API REST de Spring Boot mediante fetch().
 * ========================================================= */
"use strict";

/* ---------------------------------------------------------
 * Configuración
 * --------------------------------------------------------- */
const API_HABITACIONES = "/api/habitaciones";
const API_RESERVAS = "/api/reservas";

const TIPOS = { SENCILLA: "Sencilla", DOBLE: "Doble", TRIPLE: "Triple", SUITE: "Suite" };

const ESTADOS = {
    ACTIVA: { texto: "Activa", clase: "text-bg-success" },
    CANCELADA: { texto: "Cancelada", clase: "text-bg-secondary" },
    FINALIZADA: { texto: "Finalizada", clase: "text-bg-primary" }
};

/* Nombres amigables de los campos que devuelve la API en "errores" */
const ETIQUETAS = {
    numero: "Número",
    tipo: "Tipo",
    capacidad: "Capacidad",
    tarifaNoche: "Tarifa por noche",
    descripcion: "Descripción",
    habitacionId: "Habitación",
    nombreHuesped: "Nombre del huésped",
    documentoHuesped: "Documento",
    telefonoHuesped: "Teléfono",
    cantidadHuespedes: "Cantidad de huéspedes",
    fechaEntrada: "Fecha de entrada",
    fechaSalida: "Fecha de salida"
};

/** Error que conserva el mensaje y los errores por campo enviados por la API. */
class ErrorApi extends Error {
    constructor(mensaje, errores = {}, estado = 0) {
        super(mensaje);
        this.errores = errores;
        this.estado = estado;
    }
}

/* ---------------------------------------------------------
 * Estado de la página
 * --------------------------------------------------------- */
let habitaciones = [];
let reservas = [];
let filtroReservas = { tipo: "todas" };
let temporizadorMensaje;
let temporizadorCotizacion;

const formReserva = document.getElementById("formReserva");
const formHabitacion = document.getElementById("formHabitacion");
const formDisponibilidad = document.getElementById("formDisponibilidad");
const tablaReservas = document.getElementById("tablaReservas");
const tablaHabitaciones = document.getElementById("tablaHabitaciones");
const tablaDisponibles = document.getElementById("tablaDisponibles");

document.addEventListener("DOMContentLoaded", iniciar);

function iniciar() {
    formReserva.addEventListener("submit", guardarReserva);
    formHabitacion.addEventListener("submit", guardarHabitacion);
    formDisponibilidad.addEventListener("submit", buscarDisponibles);
    document.getElementById("formBuscarHuesped").addEventListener("submit", buscarPorHuesped);

    document.getElementById("btnLimpiarReserva").addEventListener("click", limpiarFormReserva);
    document.getElementById("btnLimpiarHabitacion").addEventListener("click", limpiarFormHabitacion);
    document.getElementById("btnRecargarHabitaciones").addEventListener("click", cargarHabitaciones);
    document.getElementById("btnTodas").addEventListener("click", () => aplicarFiltro({ tipo: "todas" }));
    document.getElementById("btnActivas").addEventListener("click", () => aplicarFiltro({ tipo: "activas" }));

    // Cada vez que cambia la habitación o las fechas se recalcula el total estimado
    ["habitacionId", "fechaEntrada", "fechaSalida"].forEach(campo => {
        formReserva.elements[campo].addEventListener("input", programarCotizacion);
        formReserva.elements[campo].addEventListener("change", programarCotizacion);
    });

    prepararFechas();
    cargarHabitaciones();
    cargarReservas();
    cargarResumen();
}

/* =========================================================
 * Comunicación con la API
 * ========================================================= */

/**
 * Hace la petición HTTP y devuelve el JSON.
 * Si la API responde con error (400, 404, 409, 422, 500...) lanza ErrorApi
 * con el mensaje que envió el backend.
 */
async function peticion(url, opciones = {}) {
    let respuesta;
    try {
        respuesta = await fetch(url, {
            ...opciones,
            headers: { "Content-Type": "application/json", "Accept": "application/json" }
        });
    } catch {
        throw new ErrorApi("No hay conexión con el servidor. Verifique que el backend esté en ejecución.");
    }

    if (respuesta.status === 204) {
        return null;
    }

    const texto = await respuesta.text();
    let datos = null;
    try {
        datos = texto ? JSON.parse(texto) : null;
    } catch {
        datos = null;
    }

    if (!respuesta.ok) {
        throw new ErrorApi(
            datos?.mensaje || `Error ${respuesta.status} al comunicarse con la API`,
            datos?.errores || {},
            respuesta.status
        );
    }
    return datos;
}

/* =========================================================
 * Indicadores
 * ========================================================= */

async function cargarResumen() {
    try {
        const r = await peticion(`${API_RESERVAS}/resumen`);
        document.getElementById("indReservasActivas").textContent = r.reservasActivas;
        document.getElementById("indReservasDetalle").textContent =
            `${r.reservasFinalizadas} finalizadas · ${r.reservasCanceladas} canceladas`;
        document.getElementById("indOcupacion").textContent = `${Number(r.porcentajeOcupacionHoy).toFixed(1)} %`;
        document.getElementById("indOcupacionDetalle").textContent =
            `${r.habitacionesOcupadasHoy} de ${r.habitacionesActivas} habitaciones ocupadas`;
        document.getElementById("indHabitaciones").textContent = r.habitacionesActivas;
        document.getElementById("indHabitacionesDetalle").textContent = `${r.totalHabitaciones} registradas`;
        document.getElementById("indIngreso").textContent = formatoMoneda(r.ingresoEstimadoActivas);
    } catch (error) {
        console.warn("No se pudo cargar el resumen:", error.message);
    }
}

/* =========================================================
 * Habitaciones
 * ========================================================= */

async function cargarHabitaciones() {
    try {
        habitaciones = await peticion(API_HABITACIONES);
        mostrarHabitaciones();
        llenarSelectHabitaciones();
    } catch (error) {
        tablaHabitaciones.innerHTML = filaMensaje(6, error.message, "text-danger");
        mostrarError(error);
    }
}

function mostrarHabitaciones() {
    if (habitaciones.length === 0) {
        tablaHabitaciones.innerHTML = filaMensaje(6, "No hay habitaciones registradas.");
        return;
    }

    tablaHabitaciones.innerHTML = habitaciones.map(h => `
        <tr class="${h.activa ? "" : "text-secondary"}">
            <td>
                <span class="fw-semibold">${escaparHTML(h.numero)}</span><br>
                <small class="text-secondary">${escaparHTML(h.descripcion || "")}</small>
            </td>
            <td>${TIPOS[h.tipo] || escaparHTML(h.tipo)}</td>
            <td class="text-center">${h.capacidad}</td>
            <td class="text-end text-nowrap">${formatoMoneda(h.tarifaNoche)}</td>
            <td>${h.activa
                ? '<span class="badge text-bg-success">Activa</span>'
                : '<span class="badge text-bg-secondary">Inactiva</span>'}</td>
            <td class="text-end text-nowrap">
                <button type="button" class="btn btn-sm btn-outline-primary" onclick="editarHabitacion(${h.id})">Editar</button>
                <button type="button" class="btn btn-sm btn-outline-secondary" onclick="verReservasDeHabitacion(${h.id})">Reservas</button>
                ${h.activa
                    ? `<button type="button" class="btn btn-sm btn-outline-danger" onclick="desactivarHabitacion(${h.id})">Desactivar</button>`
                    : `<button type="button" class="btn btn-sm btn-outline-success" onclick="activarHabitacion(${h.id})">Activar</button>`}
            </td>
        </tr>`).join("");
}

/** Solo las habitaciones activas pueden recibir reservas. */
function llenarSelectHabitaciones() {
    const select = formReserva.elements.habitacionId;
    const seleccionada = select.value;

    select.innerHTML = `<option value="">Seleccione una habitación</option>` +
        habitaciones
            .filter(h => h.activa)
            .map(h => `<option value="${h.id}">${escaparHTML(h.numero)} · ${TIPOS[h.tipo]} · ${h.capacidad} pers. · ${formatoMoneda(h.tarifaNoche)}/noche</option>`)
            .join("");

    select.value = seleccionada;
}

/** POST (nueva) o PUT (modificar) según exista idEdicion. */
async function guardarHabitacion(evento) {
    evento.preventDefault();
    limpiarErrores(formHabitacion);

    const campos = formHabitacion.elements;
    const id = campos.idEdicion.value;
    const datos = {
        numero: campos.numero.value.trim(),
        tipo: campos.tipo.value || null,
        capacidad: numeroONulo(campos.capacidad.value),
        tarifaNoche: numeroONulo(campos.tarifaNoche.value),
        descripcion: campos.descripcion.value.trim() || null
    };

    try {
        await peticion(id ? `${API_HABITACIONES}/${id}` : API_HABITACIONES, {
            method: id ? "PUT" : "POST",
            body: JSON.stringify(datos)
        });
        mostrarMensaje(id ? "Habitación actualizada correctamente." : "Habitación registrada correctamente.");
        limpiarFormHabitacion();
        await Promise.all([cargarHabitaciones(), cargarResumen()]);
    } catch (error) {
        mostrarError(error, formHabitacion);
    }
}

function editarHabitacion(id) {
    const habitacion = habitaciones.find(h => h.id === id);
    if (!habitacion) {
        return;
    }
    limpiarErrores(formHabitacion);

    const campos = formHabitacion.elements;
    campos.idEdicion.value = habitacion.id;
    campos.numero.value = habitacion.numero;
    campos.tipo.value = habitacion.tipo;
    campos.capacidad.value = habitacion.capacidad;
    campos.tarifaNoche.value = habitacion.tarifaNoche;
    campos.descripcion.value = habitacion.descripcion || "";

    document.getElementById("tituloFormHabitacion").textContent = `Modificar habitación ${habitacion.numero}`;
    document.getElementById("btnGuardarHabitacion").textContent = "Actualizar habitación";
    formHabitacion.scrollIntoView({ behavior: "smooth", block: "start" });
}

/** DELETE = eliminación lógica (desactivar). */
async function desactivarHabitacion(id) {
    const habitacion = habitaciones.find(h => h.id === id);
    if (!confirm(`¿Desactivar la habitación ${habitacion ? habitacion.numero : id}? Dejará de aceptar reservas.`)) {
        return;
    }
    try {
        await peticion(`${API_HABITACIONES}/${id}`, { method: "DELETE" });
        mostrarMensaje("Habitación desactivada correctamente.");
        await Promise.all([cargarHabitaciones(), cargarResumen()]);
    } catch (error) {
        mostrarError(error);
    }
}

async function activarHabitacion(id) {
    try {
        const habitacion = await peticion(`${API_HABITACIONES}/${id}/activar`, { method: "PATCH" });
        mostrarMensaje(`Habitación ${habitacion.numero} activada correctamente.`);
        await Promise.all([cargarHabitaciones(), cargarResumen()]);
    } catch (error) {
        mostrarError(error);
    }
}

/** Muestra en la pestaña Reservas solo las reservas de esa habitación (relación 1:N). */
function verReservasDeHabitacion(id) {
    const habitacion = habitaciones.find(h => h.id === id);
    aplicarFiltro({ tipo: "habitacion", valor: id, etiqueta: habitacion ? habitacion.numero : id });
    mostrarPestana("reservas");
}

function limpiarFormHabitacion() {
    formHabitacion.reset();
    formHabitacion.elements.idEdicion.value = "";
    limpiarErrores(formHabitacion);
    document.getElementById("tituloFormHabitacion").textContent = "Registrar habitación";
    document.getElementById("btnGuardarHabitacion").textContent = "Guardar habitación";
}

/* =========================================================
 * Reservas
 * ========================================================= */

function aplicarFiltro(filtro) {
    filtroReservas = filtro;
    cargarReservas();
}

/** Devuelve [url, descripción] según el filtro elegido. */
function urlSegunFiltro() {
    switch (filtroReservas.tipo) {
        case "activas":
            return [`${API_RESERVAS}/activas`, "Reservas activas"];
        case "huesped":
            return [`${API_RESERVAS}/huesped?texto=${encodeURIComponent(filtroReservas.valor)}`,
                `Reservas del huésped: "${filtroReservas.valor}"`];
        case "habitacion":
            return [`${API_HABITACIONES}/${filtroReservas.valor}/reservas`,
                `Reservas de la habitación ${filtroReservas.etiqueta}`];
        default:
            return [API_RESERVAS, "Todas las reservas"];
    }
}

async function cargarReservas() {
    const [url, descripcion] = urlSegunFiltro();
    document.getElementById("filtroActual").textContent = descripcion;
    document.getElementById("btnTodas").classList.toggle("active", filtroReservas.tipo === "todas");
    document.getElementById("btnActivas").classList.toggle("active", filtroReservas.tipo === "activas");

    tablaReservas.innerHTML = filaMensaje(8, "Cargando reservas...");
    try {
        reservas = await peticion(url);
        mostrarReservas();
    } catch (error) {
        tablaReservas.innerHTML = filaMensaje(8, error.message, "text-danger");
        mostrarError(error);
    }
}

function mostrarReservas() {
    if (reservas.length === 0) {
        tablaReservas.innerHTML = filaMensaje(8, "No hay reservas para este filtro.");
        return;
    }

    const hoy = hoyISO();
    tablaReservas.innerHTML = reservas.map(r => {
        const estado = ESTADOS[r.estado] || { texto: r.estado, clase: "text-bg-light" };
        const activa = r.estado === "ACTIVA";
        const yaComenzo = r.fechaEntrada <= hoy;   // fechas ISO se pueden comparar como texto

        const acciones = activa
            ? `<button type="button" class="btn btn-sm btn-outline-primary" onclick="editarReserva(${r.id})">Editar</button>
               ${yaComenzo ? `<button type="button" class="btn btn-sm btn-outline-success" onclick="finalizarReserva(${r.id})">Finalizar</button>` : ""}
               <button type="button" class="btn btn-sm btn-outline-danger" onclick="cancelarReserva(${r.id})">Cancelar</button>`
            : `<span class="text-secondary small">Sin acciones</span>`;

        return `
        <tr>
            <td class="text-secondary">#${r.id}</td>
            <td>
                <span class="fw-semibold">${escaparHTML(r.numeroHabitacion)}</span><br>
                <small class="text-secondary">${TIPOS[r.tipoHabitacion] || ""}</small>
            </td>
            <td>
                ${escaparHTML(r.nombreHuesped)}<br>
                <small class="text-secondary">${escaparHTML(r.documentoHuesped)} · ${r.cantidadHuespedes} pers.</small>
            </td>
            <td class="text-nowrap">
                ${formatoFecha(r.fechaEntrada)}<br>
                <small class="text-secondary">al ${formatoFecha(r.fechaSalida)}</small>
            </td>
            <td class="text-center">${r.noches}</td>
            <td class="text-end text-nowrap fw-semibold">${formatoMoneda(r.total)}</td>
            <td><span class="badge ${estado.clase}">${estado.texto}</span></td>
            <td class="text-end text-nowrap">${acciones}</td>
        </tr>`;
    }).join("");
}

/** Filtro: reservas por huésped. */
function buscarPorHuesped(evento) {
    evento.preventDefault();
    const texto = evento.target.elements.texto.value.trim();
    if (!texto) {
        mostrarMensaje("Escriba el nombre o el documento del huésped que desea buscar.", "warning");
        return;
    }
    aplicarFiltro({ tipo: "huesped", valor: texto });
}

/** POST (nueva) o PUT (modificar). El total lo calcula el backend. */
async function guardarReserva(evento) {
    evento.preventDefault();
    limpiarErrores(formReserva);

    const campos = formReserva.elements;
    const id = campos.idEdicion.value;
    const datos = {
        habitacionId: numeroONulo(campos.habitacionId.value),
        nombreHuesped: campos.nombreHuesped.value.trim(),
        documentoHuesped: campos.documentoHuesped.value.trim(),
        telefonoHuesped: campos.telefonoHuesped.value.trim() || null,
        cantidadHuespedes: numeroONulo(campos.cantidadHuespedes.value),
        fechaEntrada: campos.fechaEntrada.value || null,
        fechaSalida: campos.fechaSalida.value || null
    };

    try {
        const reserva = await peticion(id ? `${API_RESERVAS}/${id}` : API_RESERVAS, {
            method: id ? "PUT" : "POST",
            body: JSON.stringify(datos)
        });
        mostrarMensaje(`${id ? "Reserva actualizada" : "Reserva registrada"} correctamente: habitación ` +
            `${reserva.numeroHabitacion}, ${reserva.noches} noche(s), total ${formatoMoneda(reserva.total)}.`);
        limpiarFormReserva();
        await Promise.all([cargarReservas(), cargarResumen()]);
    } catch (error) {
        mostrarError(error, formReserva);
    }
}

function editarReserva(id) {
    const reserva = reservas.find(r => r.id === id);
    if (!reserva) {
        return;
    }
    limpiarErrores(formReserva);

    const campos = formReserva.elements;
    campos.idEdicion.value = reserva.id;
    campos.habitacionId.value = reserva.habitacionId;
    campos.nombreHuesped.value = reserva.nombreHuesped;
    campos.documentoHuesped.value = reserva.documentoHuesped;
    campos.telefonoHuesped.value = reserva.telefonoHuesped || "";
    campos.cantidadHuespedes.value = reserva.cantidadHuespedes;
    campos.fechaEntrada.min = "";   // una estadía en curso conserva su entrada pasada
    campos.fechaEntrada.value = reserva.fechaEntrada;
    campos.fechaSalida.value = reserva.fechaSalida;

    document.getElementById("tituloFormReserva").textContent = `Modificar reserva #${reserva.id}`;
    document.getElementById("btnGuardarReserva").textContent = "Actualizar reserva";
    mostrarPestana("reservas");
    formReserva.scrollIntoView({ behavior: "smooth", block: "start" });
    programarCotizacion();
}

function cancelarReserva(id) {
    cambiarEstadoReserva(id, "cancelar", "cancelada");
}

function finalizarReserva(id) {
    cambiarEstadoReserva(id, "finalizar", "finalizada");
}

/** PATCH /api/reservas/{id}/cancelar  o  /finalizar */
async function cambiarEstadoReserva(id, accion, participio) {
    const reserva = reservas.find(r => r.id === id);
    const quien = reserva ? ` de ${reserva.nombreHuesped}` : "";
    if (!confirm(`¿Desea ${accion} la reserva #${id}${quien}?`)) {
        return;
    }
    try {
        await peticion(`${API_RESERVAS}/${id}/${accion}`, { method: "PATCH" });
        mostrarMensaje(`Reserva #${id} ${participio} correctamente.`);
        if (formReserva.elements.idEdicion.value === String(id)) {
            limpiarFormReserva();
        }
        await Promise.all([cargarReservas(), cargarResumen()]);
    } catch (error) {
        mostrarError(error);
    }
}

function limpiarFormReserva() {
    formReserva.reset();
    formReserva.elements.idEdicion.value = "";
    formReserva.elements.fechaEntrada.min = hoyISO();
    limpiarErrores(formReserva);
    document.getElementById("tituloFormReserva").textContent = "Nueva reserva";
    document.getElementById("btnGuardarReserva").textContent = "Guardar reserva";
    actualizarCotizacion();
}

/* ---------- Total estimado antes de guardar ---------- */

function programarCotizacion() {
    clearTimeout(temporizadorCotizacion);
    temporizadorCotizacion = setTimeout(actualizarCotizacion, 250);
}

async function actualizarCotizacion() {
    const campos = formReserva.elements;
    const caja = document.getElementById("cotizacion");

    if (!campos.habitacionId.value || !campos.fechaEntrada.value || !campos.fechaSalida.value) {
        caja.className = "cotizacion mb-3";
        caja.textContent = "Seleccione habitación y fechas para calcular el total estimado.";
        return;
    }

    const parametros = new URLSearchParams({
        habitacionId: campos.habitacionId.value,
        entrada: campos.fechaEntrada.value,
        salida: campos.fechaSalida.value
    });
    if (campos.idEdicion.value) {
        parametros.append("reservaId", campos.idEdicion.value);
    }

    try {
        const c = await peticion(`${API_RESERVAS}/cotizar?${parametros}`);
        caja.className = `cotizacion mb-3 ${c.disponible ? "cotizacion-ok" : "cotizacion-error"}`;
        caja.innerHTML = `
            <div class="d-flex justify-content-between">
                <span>${c.noches} noche(s) × ${formatoMoneda(c.tarifaNoche)}</span>
                <strong>${formatoMoneda(c.totalEstimado)}</strong>
            </div>
            <small>${c.disponible ? "✔ " : "✖ "}${escaparHTML(c.mensaje)}</small>`;
    } catch (error) {
        caja.className = "cotizacion mb-3 cotizacion-error";
        caja.textContent = error.message;
    }
}

/* =========================================================
 * Disponibilidad por periodo
 * ========================================================= */

async function buscarDisponibles(evento) {
    evento.preventDefault();
    const campos = formDisponibilidad.elements;
    const parametros = new URLSearchParams({
        entrada: campos.entrada.value,
        salida: campos.salida.value,
        huespedes: campos.huespedes.value || "1"
    });
    const resultado = document.getElementById("resultadoDisponibilidad");

    tablaDisponibles.innerHTML = filaMensaje(7, "Buscando...");
    try {
        const lista = await peticion(`${API_HABITACIONES}/disponibles?${parametros}`);
        resultado.textContent = `${lista.length} habitación(es) disponible(s) del ` +
            `${formatoFecha(campos.entrada.value)} al ${formatoFecha(campos.salida.value)}`;

        tablaDisponibles.innerHTML = lista.length === 0
            ? filaMensaje(7, "No hay habitaciones libres para ese periodo y cantidad de huéspedes.")
            : lista.map(h => `
                <tr>
                    <td class="fw-semibold">${escaparHTML(h.numero)}</td>
                    <td>${TIPOS[h.tipo] || escaparHTML(h.tipo)}</td>
                    <td class="text-center">${h.capacidad}</td>
                    <td class="text-end text-nowrap">${formatoMoneda(h.tarifaNoche)}</td>
                    <td class="text-center">${h.noches}</td>
                    <td class="text-end text-nowrap fw-semibold">${formatoMoneda(h.totalEstimado)}</td>
                    <td class="text-end">
                        <button type="button" class="btn btn-sm btn-principal" onclick="reservarDesdeDisponibilidad(${h.habitacionId})">Reservar</button>
                    </td>
                </tr>`).join("");
    } catch (error) {
        resultado.textContent = "";
        tablaDisponibles.innerHTML = filaMensaje(7, error.message, "text-danger");
        mostrarError(error);
    }
}

/** Lleva la habitación y las fechas elegidas al formulario de reserva. */
function reservarDesdeDisponibilidad(habitacionId) {
    const disponibilidad = formDisponibilidad.elements;
    limpiarFormReserva();

    const campos = formReserva.elements;
    campos.habitacionId.value = habitacionId;
    campos.fechaEntrada.value = disponibilidad.entrada.value;
    campos.fechaSalida.value = disponibilidad.salida.value;
    campos.cantidadHuespedes.value = disponibilidad.huespedes.value || 1;

    mostrarPestana("reservas");
    campos.nombreHuesped.focus();
    programarCotizacion();
}

/* =========================================================
 * Mensajes y errores
 * ========================================================= */

function mostrarMensaje(texto, tipo = "success", errores = {}) {
    const detalle = Object.entries(errores)
        .map(([campo, mensaje]) => `<li><strong>${escaparHTML(ETIQUETAS[campo] || campo)}:</strong> ${escaparHTML(mensaje)}</li>`)
        .join("");

    const contenedor = document.getElementById("mensaje");
    contenedor.innerHTML = `
        <div class="alert alert-${tipo} alert-dismissible fade show shadow" role="alert">
            ${escaparHTML(texto)}
            ${detalle ? `<ul class="mb-0 mt-2 small">${detalle}</ul>` : ""}
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Cerrar"></button>
        </div>`;

    clearTimeout(temporizadorMensaje);
    temporizadorMensaje = setTimeout(() => { contenedor.innerHTML = ""; }, tipo === "success" ? 4000 : 9000);
}

/** Muestra el error de la API y marca en rojo los campos inválidos del formulario. */
function mostrarError(error, formulario) {
    mostrarMensaje(error.message, "danger", error.errores || {});
    if (formulario) {
        marcarErrores(formulario, error.errores || {});
    }
}

function marcarErrores(formulario, errores) {
    Object.entries(errores).forEach(([campo, mensaje]) => {
        const elemento = formulario.elements[campo];
        if (!elemento) {
            return;
        }
        elemento.classList.add("is-invalid");
        const aviso = document.createElement("div");
        aviso.className = "invalid-feedback";
        aviso.textContent = mensaje;
        elemento.insertAdjacentElement("afterend", aviso);
    });
}

function limpiarErrores(formulario) {
    formulario.querySelectorAll(".is-invalid").forEach(e => e.classList.remove("is-invalid"));
    formulario.querySelectorAll(".invalid-feedback").forEach(e => e.remove());
}

/* =========================================================
 * Utilidades
 * ========================================================= */

function mostrarPestana(nombre) {
    bootstrap.Tab.getOrCreateInstance(document.getElementById(`tab-${nombre}`)).show();
}

function prepararFechas() {
    const hoy = hoyISO();
    formReserva.elements.fechaEntrada.min = hoy;
    formDisponibilidad.elements.entrada.value = hoy;
    formDisponibilidad.elements.salida.value = sumarDias(hoy, 1);
}

function filaMensaje(columnas, texto, clase = "text-secondary") {
    return `<tr><td colspan="${columnas}" class="text-center py-4 ${clase}">${escaparHTML(texto)}</td></tr>`;
}

function numeroONulo(valor) {
    return valor === "" || valor == null ? null : Number(valor);
}

const formatoQuetzales = new Intl.NumberFormat("es-GT", { style: "currency", currency: "GTQ" });

function formatoMoneda(valor) {
    return formatoQuetzales.format(Number(valor ?? 0));
}

/** "2026-11-20" -> "20/11/2026" */
function formatoFecha(iso) {
    if (!iso) {
        return "—";
    }
    const [anio, mes, dia] = iso.split("-");
    return `${dia}/${mes}/${anio}`;
}

/** Fecha de hoy en formato AAAA-MM-DD según la hora local. */
function hoyISO() {
    const fecha = new Date();
    fecha.setMinutes(fecha.getMinutes() - fecha.getTimezoneOffset());
    return fecha.toISOString().slice(0, 10);
}

function sumarDias(iso, dias) {
    const fecha = new Date(`${iso}T00:00:00`);
    fecha.setDate(fecha.getDate() + dias);
    const mes = String(fecha.getMonth() + 1).padStart(2, "0");
    const dia = String(fecha.getDate()).padStart(2, "0");
    return `${fecha.getFullYear()}-${mes}-${dia}`;
}

/** Evita que el texto recibido se interprete como HTML. */
function escaparHTML(valor) {
    return String(valor ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}
