package ar.edu.utn.frba.tps.auditoria.service;

import ar.edu.utn.frba.tps.auditoria.config.AuditoriaProperties;
import ar.edu.utn.frba.tps.auditoria.dto.AuditEventMessage;
import ar.edu.utn.frba.tps.auditoria.exception.MessagingUnavailableException;
import ar.edu.utn.frba.tps.auditoria.model.AuditEvent;
import ar.edu.utn.frba.tps.auditoria.repository.AuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

/**
 * Procesa un evento recibido de la cola: espera la demora, confirma con el modulo de mensajeria
 * que el evento siga pendiente y, si es asi, lo registra. Lo registrado vive solo en Auditoria.
 */
@Service
public class AuditEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(AuditEventProcessor.class);
    private static final String PENDING = "PENDING";
    private static final String CANCELLED = "CANCELLED";

    private final MessagingClient messagingClient;
    private final AuditEventRepository repository;
    private final AuditoriaProperties properties;
    private final Clock clock;

    public AuditEventProcessor(MessagingClient messagingClient, AuditEventRepository repository,
                               AuditoriaProperties properties, Clock clock) {
        this.messagingClient = messagingClient;
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    public void process(AuditEventMessage event) {
        if (repository.existsById(event.id())) {
            log.info("Evento {} ya registrado: se ignora la reentrega", event.id());
            return;
        }

        // Mientras corre la demora el evento queda pendiente: esa es la ventana en la que se lo
        // puede dar de baja antes de que Auditoria lo registre.
        delay();

        Optional<AuditEventMessage> current = messagingClient.findPending(event.id());
        if (current.isEmpty()) {
            log.info("Evento {} ya no esta en pendientes: se descarta sin registrar", event.id());
            return;
        }

        AuditEventMessage confirmed = current.get();
        if (CANCELLED.equals(confirmed.status())) {
            log.info("Evento {} quedo en estado CANCELLED: se descarta sin registrar", event.id());
            return;
        }
        if (!PENDING.equals(confirmed.status())) {
            // Estado que el contrato no define: no se descarta en silencio, se reintenta.
            throw new MessagingUnavailableException(
                    "Estado inesperado para el evento " + event.id() + ": " + confirmed.status());
        }

        // Se registra con los datos que devuelve el modulo (la fuente de verdad), no con los del
        // mensaje de la cola: asi un mensaje adulterado con el id de un pendiente real no altera
        // lo que se audita.
        repository.save(new AuditEvent(confirmed.id(), confirmed.type(), confirmed.cuit(),
                confirmed.timestamp(), clock.instant()));
        log.info("Evento {} registrado en auditoria", event.id());
    }

    private void delay() {
        Duration delay = properties.delay();
        if (delay == null || delay.isZero() || delay.isNegative()) {
            return;
        }
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Se interrumpio la demora del procesamiento", e);
        }
    }

}
