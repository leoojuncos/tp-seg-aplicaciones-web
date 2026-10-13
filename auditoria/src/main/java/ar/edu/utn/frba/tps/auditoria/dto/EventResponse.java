package ar.edu.utn.frba.tps.auditoria.dto;

import java.time.Instant;
import java.util.UUID;

/** Un evento de auditoria registrado, como lo devuelve la API de Auditoria. */
public record EventResponse(UUID id, String type, String cuit, Instant occurredAt, Instant registeredAt) {
}
