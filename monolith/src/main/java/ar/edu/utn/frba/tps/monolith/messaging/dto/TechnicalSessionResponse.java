package ar.edu.utn.frba.tps.monolith.messaging.dto;

import ar.edu.utn.frba.tps.monolith.messaging.model.TechnicalRole;

import java.time.Instant;

public record TechnicalSessionResponse(String token, String username, TechnicalRole role, Instant expiresAt) {
}
