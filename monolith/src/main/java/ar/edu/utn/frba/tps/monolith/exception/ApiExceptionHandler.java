package ar.edu.utn.frba.tps.monolith.exception;

import ar.edu.utn.frba.tps.monolith.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduce los errores de los controllers del monolito al formato de error del SGM. Los del modulo
 * messaging los resuelve primero su propio handler, que tiene precedencia; lo que ese no maneja llega
 * aca. Los errores de Spring MVC (ruta inexistente, verbo o content-type no aceptados, cuerpo o
 * parametro invalido) los resuelve ResponseEntityExceptionHandler con su status, y aca solo se cambia
 * el cuerpo.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private static final String VALIDATION_MESSAGE = "Faltan campos o tienen un formato incorrecto";
    private static final String INTERNAL_MESSAGE = "Error interno del servidor";

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(NotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "not_found", exception.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> forbidden(ForbiddenException exception) {
        return error(HttpStatus.FORBIDDEN, "forbidden", exception.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> conflict(ConflictException exception) {
        return error(HttpStatus.CONFLICT, "conflict", exception.getMessage());
    }

    // Con @Validated en la clase, la validacion de parametros la hace AOP y no pasa por
    // ResponseEntityExceptionHandler.
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> constraintViolation() {
        return error(HttpStatus.BAD_REQUEST, "validation", VALIDATION_MESSAGE);
    }

    // Lo inesperado: el detalle va solo al log.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpected(Exception exception, HttpServletRequest request) {
        log.error("Error inesperado en {} {}", request.getMethod(), request.getRequestURI(), exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "internal", INTERNAL_MESSAGE);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        if (statusCode.is5xxServerError()) {
            log.error("Error de Spring MVC en {}", request.getDescription(false), exception);
        }
        return ResponseEntity.status(statusCode).headers(headers).body(springMvcError(statusCode));
    }

    private static ErrorResponse springMvcError(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> new ErrorResponse("validation", VALIDATION_MESSAGE);
            case 401 -> new ErrorResponse("unauthorized", "No hay sesión, o no es válida");
            case 403 -> new ErrorResponse("forbidden", "La sesión no tiene permiso para esta operación");
            case 404 -> new ErrorResponse("not_found", "La ruta no existe");
            case 405 -> new ErrorResponse("method_not_allowed", "La ruta no admite ese verbo HTTP");
            case 409 -> new ErrorResponse("conflict", "La operación choca con el estado actual del recurso");
            case 415 -> new ErrorResponse("unsupported_media_type", "El cuerpo tiene que ser JSON");
            default -> status.is5xxServerError()
                    ? new ErrorResponse("internal", INTERNAL_MESSAGE)
                    : new ErrorResponse("bad_request", "No se pudo procesar el pedido");
        };
    }

    private static ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(code, message));
    }

}
