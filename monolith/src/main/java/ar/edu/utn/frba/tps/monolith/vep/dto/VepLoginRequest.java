package ar.edu.utn.frba.tps.monolith.vep.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Ingreso al portal: el CUIT con sus 11 digitos, sin guiones, y la clave. */
public record VepLoginRequest(@NotBlank @Pattern(regexp = "\\d{11}") String cuit, @NotBlank String password) {
}
