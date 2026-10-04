package ar.edu.utn.frba.tps.monolith.messaging.dto;

import ar.edu.utn.frba.tps.monolith.messaging.model.TechnicalRole;

public record TechnicalAccountResponse(String username, TechnicalRole role, String serviceId) {
}
