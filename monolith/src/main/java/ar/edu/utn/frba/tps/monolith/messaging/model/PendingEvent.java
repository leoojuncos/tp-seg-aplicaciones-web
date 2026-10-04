package ar.edu.utn.frba.tps.monolith.messaging.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/** Evento de auditoria entregado al modulo. Esta tabla, y no la cola, define si sigue pendiente. */
@Entity
@Table(name = "pending_events")
public class PendingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String type;

    private String cuit;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    private PendingEventStatus status;

    protected PendingEvent() {
    }

    public PendingEvent(String type, String cuit, Instant occurredAt) {
        this.type = type;
        this.cuit = cuit;
        // Postgres guarda microsegundos: se trunca aca para que el evento encolado y el persistido
        // tengan exactamente el mismo timestamp.
        this.occurredAt = occurredAt.truncatedTo(ChronoUnit.MICROS);
        this.status = PendingEventStatus.PENDING;
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

    public PendingEventStatus getStatus() {
        return status;
    }

}
