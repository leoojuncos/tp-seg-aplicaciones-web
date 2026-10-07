package ar.edu.utn.frba.tps.auditoria.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento de auditoria con el formato de docs/contracts.md. Es el cuerpo del mensaje que llega por
 * la cola y tambien la respuesta de la API de pendientes del modulo de mensajeria.
 */
public record AuditEventMessage(UUID id, String type, String cuit, Instant timestamp, String status) {
}
