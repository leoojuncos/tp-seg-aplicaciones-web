package ar.edu.utn.frba.tps.monolith.exception;

/** No hay sesion, o las credenciales no son validas. ApiExceptionHandler responde 401 {@code unauthorized} con este mensaje. */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }

}
