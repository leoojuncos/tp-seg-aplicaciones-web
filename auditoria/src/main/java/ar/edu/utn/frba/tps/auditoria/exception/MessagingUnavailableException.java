package ar.edu.utn.frba.tps.auditoria.exception;

/**
 * El modulo de mensajeria no dio una respuesta firme sobre el estado del evento (esta caido,
 * devolvio un error del servidor o el token sigue sin servir tras reautenticar). Se propaga para
 * que el consumo se reintente: asi un corte transitorio no hace perder el registro de auditoria.
 */
public class MessagingUnavailableException extends RuntimeException {

    public MessagingUnavailableException(String message) {
        super(message);
    }

    public MessagingUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

}
