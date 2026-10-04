package ar.edu.utn.frba.tps.monolith.messaging.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String username, @NotBlank String password) {
}
