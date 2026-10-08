package ar.edu.utn.frba.tps.auditoria.dto;

/**
 * Copia del de monolith: Auditoria responde con el mismo formato de error del SGM. {@code error} es un
 * codigo estable que el front puede comparar y {@code message}, un detalle para mostrar. Los codigos
 * estan en docs/contracts.md.
 */
public record ErrorResponse(String error, String message) {
}
