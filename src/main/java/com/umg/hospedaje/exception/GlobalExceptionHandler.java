package com.umg.hospedaje.exception;

import com.umg.hospedaje.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manejo centralizado de errores.
 * Convierte cada excepción en una respuesta JSON (ErrorResponseDTO) con el código HTTP adecuado:
 *
 *   400 Bad Request          -> datos o parámetros inválidos
 *   404 Not Found            -> registro o ruta inexistente
 *   405 Method Not Allowed   -> método HTTP no soportado en la ruta
 *   409 Conflict             -> duplicados, reservas traslapadas, restricciones de la BD
 *   422 Unprocessable        -> regla de negocio incumplida
 *   500 Internal Server Error-> error inesperado
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ------------------------------------------------------------ excepciones propias

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponseDTO> manejarNoEncontrado(RecursoNoEncontradoException error,
                                                                HttpServletRequest request) {
        return crearRespuesta(HttpStatus.NOT_FOUND, "Recurso no encontrado", error.getMessage(), request);
    }

    @ExceptionHandler(RegistroDuplicadoException.class)
    public ResponseEntity<ErrorResponseDTO> manejarDuplicado(RegistroDuplicadoException error,
                                                             HttpServletRequest request) {
        return crearRespuesta(HttpStatus.CONFLICT, "Registro duplicado", error.getMessage(), request);
    }

    @ExceptionHandler(ReservaTraslapadaException.class)
    public ResponseEntity<ErrorResponseDTO> manejarTraslape(ReservaTraslapadaException error,
                                                            HttpServletRequest request) {
        return crearRespuesta(HttpStatus.CONFLICT, "Fechas no disponibles", error.getMessage(), request);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponseDTO> manejarReglaNegocio(ReglaNegocioException error,
                                                                HttpServletRequest request) {
        return crearRespuesta(HttpStatus.UNPROCESSABLE_CONTENT, "Regla de negocio incumplida",
                error.getMessage(), request);
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<ErrorResponseDTO> manejarSolicitudInvalida(SolicitudInvalidaException error,
                                                                     HttpServletRequest request) {
        return crearRespuesta(HttpStatus.BAD_REQUEST, "Solicitud inválida", error.getMessage(), request);
    }

    // ------------------------------------------------------------ validaciones y formato

    /** Errores de @Valid en los DTO: devuelve un mensaje por cada campo. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> manejarValidacion(MethodArgumentNotValidException error,
                                                              HttpServletRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        error.getBindingResult()
                .getFieldErrors()
                .forEach(campo -> errores.putIfAbsent(campo.getField(), campo.getDefaultMessage()));

        return crearRespuesta(HttpStatus.BAD_REQUEST, "Datos inválidos",
                "Los datos enviados no son válidos. Revise los campos indicados.", request, errores);
    }

    /** JSON mal escrito, fecha con formato incorrecto o valor de enum no permitido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> manejarJsonInvalido(HttpMessageNotReadableException error,
                                                                HttpServletRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();

        if (error.getCause() instanceof MismatchedInputException detalle && !detalle.getPath().isEmpty()) {
            String campo = detalle.getPath().get(detalle.getPath().size() - 1).getPropertyName();
            Class<?> tipo = detalle.getTargetType();

            String explicacion = "Valor con formato incorrecto";
            if (tipo != null && tipo.isEnum()) {
                explicacion = "Valor no permitido. Use uno de: " + Arrays.toString(tipo.getEnumConstants());
            } else if (tipo == LocalDate.class) {
                explicacion = "Fecha inválida. Use el formato AAAA-MM-DD";
            } else if (tipo != null && Number.class.isAssignableFrom(tipo)) {
                explicacion = "Debe ser un número";
            }
            if (campo != null) {
                errores.put(campo, explicacion);
            }
        }

        return crearRespuesta(HttpStatus.BAD_REQUEST, "Formato inválido",
                "El cuerpo de la solicitud no tiene un formato válido. Revise fechas (AAAA-MM-DD), números y valores permitidos.",
                request, errores);
    }

    /** Parámetro de la URL con tipo incorrecto, por ejemplo /api/reservas/abc */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> manejarTipoIncorrecto(MethodArgumentTypeMismatchException error,
                                                                  HttpServletRequest request) {
        String mensaje = (error.getRequiredType() == LocalDate.class)
                ? "El parámetro '" + error.getName() + "' debe ser una fecha con formato AAAA-MM-DD"
                : "El parámetro '" + error.getName() + "' tiene un valor inválido: " + error.getValue();

        return crearRespuesta(HttpStatus.BAD_REQUEST, "Parámetro inválido", mensaje, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDTO> manejarParametroFaltante(MissingServletRequestParameterException error,
                                                                     HttpServletRequest request) {
        return crearRespuesta(HttpStatus.BAD_REQUEST, "Parámetro faltante",
                "Falta el parámetro obligatorio '" + error.getParameterName() + "'", request);
    }

    // ------------------------------------------------------------ rutas y base de datos

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDTO> manejarRutaInexistente(NoResourceFoundException error,
                                                                   HttpServletRequest request) {
        return crearRespuesta(HttpStatus.NOT_FOUND, "Ruta no encontrada",
                "La ruta solicitada no existe", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> manejarMetodoNoPermitido(HttpRequestMethodNotSupportedException error,
                                                                     HttpServletRequest request) {
        return crearRespuesta(HttpStatus.METHOD_NOT_ALLOWED, "Método no permitido",
                "El método " + error.getMethod() + " no está permitido en esta ruta", request);
    }

    /** Respaldo: la base de datos rechazó la operación por una restricción (UNIQUE, FK, CHECK, EXCLUDE). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> manejarIntegridad(DataIntegrityViolationException error,
                                                              HttpServletRequest request) {
        log.warn("Restricción de base de datos: {}", error.getMostSpecificCause().getMessage());
        return crearRespuesta(HttpStatus.CONFLICT, "Restricción de base de datos",
                "La base de datos rechazó la operación: el dato está duplicado, la relación no existe "
                        + "o las fechas se traslapan con otra reserva activa.", request);
    }

    /** Cualquier otro error no previsto. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> manejarErrorGeneral(Exception error, HttpServletRequest request) {
        log.error("Error inesperado en {}", request.getRequestURI(), error);
        return crearRespuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
                "Ocurrió un error inesperado en el servidor. Intente de nuevo más tarde.", request);
    }

    // ------------------------------------------------------------ construcción de la respuesta

    private ResponseEntity<ErrorResponseDTO> crearRespuesta(HttpStatus estado, String titulo, String mensaje,
                                                            HttpServletRequest request) {
        return crearRespuesta(estado, titulo, mensaje, request, Map.of());
    }

    private ResponseEntity<ErrorResponseDTO> crearRespuesta(HttpStatus estado, String titulo, String mensaje,
                                                            HttpServletRequest request, Map<String, String> errores) {
        ErrorResponseDTO cuerpo = new ErrorResponseDTO(
                LocalDateTime.now(),
                estado.value(),
                titulo,
                mensaje,
                request.getRequestURI(),
                errores
        );
        return ResponseEntity.status(estado).body(cuerpo);
    }
}
