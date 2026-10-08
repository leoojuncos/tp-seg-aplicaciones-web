package ar.edu.utn.frba.tps.monolith.exception;

/** El recurso pedido no existe. ApiExceptionHandler responde 404 {@code not_found} con este mensaje. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

}
