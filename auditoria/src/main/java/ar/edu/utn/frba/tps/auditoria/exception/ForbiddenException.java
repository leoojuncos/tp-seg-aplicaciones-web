package ar.edu.utn.frba.tps.auditoria.exception;

/** La sesion no puede hacer esta operacion. ApiExceptionHandler responde 403 {@code forbidden} con este mensaje. */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }

}
