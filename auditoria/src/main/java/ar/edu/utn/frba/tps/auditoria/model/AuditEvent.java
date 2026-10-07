package ar.edu.utn.frba.tps.auditoria.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento de auditoria ya registrado. El id es el del evento original: la cola puede reentregar un
 * mensaje, y reusar su id hace que un reintento no cree un segundo registro.
 */
@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    private UUID id;

    private String type;

    private String cuit;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    @Column(name = "registered_at")
    private Instant registeredAt;

    protected AuditEvent() {
    }

    public AuditEvent(UUID id, String type, String cuit, Instant occurredAt, Instant registeredAt) {
        this.id = id;
        this.type = type;
        this.cuit = cuit;
        this.occurredAt = occurredAt;
        this.registeredAt = registeredAt;
    }

    public UUID getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getCuit() {
        return cuit;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

}
