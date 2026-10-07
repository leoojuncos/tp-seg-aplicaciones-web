package ar.edu.utn.frba.tps.auditoria.exception;

/**
 * La operacion choca con el estado actual del recurso. ApiExceptionHandler responde 409 {@code conflict}
 * con este mensaje.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

}
