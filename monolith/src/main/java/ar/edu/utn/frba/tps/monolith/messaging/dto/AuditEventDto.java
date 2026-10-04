package ar.edu.utn.frba.tps.monolith.messaging.dto;

import ar.edu.utn.frba.tps.monolith.messaging.model.PendingEventStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento de auditoria con el formato de docs/contracts.md. Es a la vez el cuerpo del mensaje que
 * va a la cola y la respuesta de la API de pendientes.
 */
public record AuditEventDto(UUID id, String type, String cuit, Instant timestamp, PendingEventStatus status) {
}
