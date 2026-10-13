package ar.edu.utn.frba.tps.monolith.messaging.dto;

import jakarta.validation.constraints.NotBlank;

public record RecoverRequest(@NotBlank String username, @NotBlank String serviceId) {
}
