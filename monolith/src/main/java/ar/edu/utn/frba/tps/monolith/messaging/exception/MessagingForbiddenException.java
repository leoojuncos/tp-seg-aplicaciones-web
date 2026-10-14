package ar.edu.utn.frba.tps.monolith.messaging.exception;

/**
 * La sesion del modulo no puede hacer esta operacion, por ejemplo dar de baja un evento con el rol
 * {@code READ}. MessagingExceptionHandler responde 403 {@code forbidden} con este mensaje.
 */
public class MessagingForbiddenException extends RuntimeException {

    public MessagingForbiddenException(String message) {
        super(message);
    }

}
