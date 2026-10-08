package ar.edu.utn.frba.tps.monolith.exception;

/**
 * La operacion choca con el estado actual del recurso, por ejemplo condonar una deuda ya condonada.
 * ApiExceptionHandler responde 409 {@code conflict} con este mensaje.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

}
