package ar.edu.utn.frba.tps.monolith.dto;

/**
 * Formato de error del SGM, el mismo en todos los modulos y en Auditoria: {@code error} es un codigo
 * estable que el front puede comparar y {@code message}, un detalle para mostrar. Los codigos estan en
 * docs/contracts.md.
 */
public record ErrorResponse(String error, String message) {
}
