package ar.edu.utn.frba.tps.monolith.messaging.exception;

import ar.edu.utn.frba.tps.monolith.messaging.controller.TechnicalAuthController;
import ar.edu.utn.frba.tps.monolith.messaging.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce los errores de los controllers del modulo al formato de error de su API. */
@RestControllerAdvice(basePackageClasses = TechnicalAuthController.class)
public class MessagingExceptionHandler {

    @ExceptionHandler(MessagingUnauthorizedException.class)
    public ResponseEntity<ErrorResponse> unauthorized(MessagingUnauthorizedException exception) {
        return error(HttpStatus.UNAUTHORIZED, "unauthorized", exception.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> invalidBody(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "validation", "Faltan campos obligatorios o el cuerpo no es un JSON correcto");
    }

    private static ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(code, message));
    }

}
