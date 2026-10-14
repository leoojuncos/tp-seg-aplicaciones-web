package ar.edu.utn.frba.tps.monolith.messaging.exception;

/**
 * La operacion choca con el estado actual del recurso, por ejemplo dar de baja un evento que ya esta
 * en {@code CANCELLED}. MessagingExceptionHandler responde 409 {@code conflict} con este mensaje.
 */
public class MessagingConflictException extends RuntimeException {

    public MessagingConflictException(String message) {
        super(message);
    }

}
