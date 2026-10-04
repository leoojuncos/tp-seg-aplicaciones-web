package ar.edu.utn.frba.tps.monolith.messaging.exception;

import ar.edu.utn.frba.tps.monolith.messaging.controller.TechnicalAuthController;
import ar.edu.utn.frba.tps.monolith.messaging.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Traduce los errores de los controllers del modulo al formato de error de su API. */
@RestControllerAdvice(basePackageClasses = TechnicalAuthController.class)
public class MessagingExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(MessagingExceptionHandler.class);

    @ExceptionHandler(MessagingUnauthorizedException.class)
    public ResponseEntity<ErrorResponse> unauthorized(MessagingUnauthorizedException exception) {
        return error(HttpStatus.UNAUTHORIZED, "unauthorized", exception.getMessage());
    }

    @ExceptionHandler(MessagingNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(MessagingNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "not_found", exception.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> badRequest() {
        return error(HttpStatus.BAD_REQUEST, "validation", "Faltan campos o tienen un formato incorrecto");
    }

    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<ErrorResponse> rabbitUnavailable(AmqpException exception) {
        log.warn("No se pudo consultar RabbitMQ: {}", exception.getMessage());
        return error(HttpStatus.SERVICE_UNAVAILABLE, "rabbitmq_unavailable", "No se pudo consultar RabbitMQ");
    }

    private static ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(code, message));
    }

}
