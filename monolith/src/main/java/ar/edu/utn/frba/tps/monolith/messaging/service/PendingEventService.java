package ar.edu.utn.frba.tps.monolith.messaging.service;

import ar.edu.utn.frba.tps.monolith.messaging.dto.AuditEventDto;
import ar.edu.utn.frba.tps.monolith.messaging.exception.MessagingConflictException;
import ar.edu.utn.frba.tps.monolith.messaging.exception.MessagingForbiddenException;
import ar.edu.utn.frba.tps.monolith.messaging.exception.MessagingNotFoundException;
import ar.edu.utn.frba.tps.monolith.messaging.mapper.MessagingMapper;
import ar.edu.utn.frba.tps.monolith.messaging.model.PendingEvent;
import ar.edu.utn.frba.tps.monolith.messaging.model.PendingEventStatus;
import ar.edu.utn.frba.tps.monolith.messaging.model.TechnicalRole;
import ar.edu.utn.frba.tps.monolith.messaging.model.TechnicalSession;
import ar.edu.utn.frba.tps.monolith.messaging.repository.PendingEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PendingEventService {

    private final PendingEventRepository pendingEventRepository;
    private final MessagingMapper mapper;

    @Autowired
    public PendingEventService(PendingEventRepository pendingEventRepository, MessagingMapper mapper) {
        this.pendingEventRepository = pendingEventRepository;
        this.mapper = mapper;
    }

    public List<AuditEventDto> list() {
        return pendingEventRepository.findAllByOrderByOccurredAtDesc().stream()
                .map(mapper::toDto)
                .toList();
    }

    public AuditEventDto get(UUID id) {
        return pendingEventRepository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> notFound(id));
    }

    /**
     * Da de baja el evento sin borrar la fila: lo pasa a CANCELLED, el estado que Auditoria descarta.
     * Solo una sesion WRITE puede hacerlo, y el rol se verifica antes que la existencia: una sesion de
     * solo lectura responde 403 aunque el evento no exista. Sin lock: si dos bajas llegan a la vez, lo
     * peor es que las dos respondan 200 (la baja no publica nada y Auditoria solo lee el estado).
     */
    @Transactional
    public AuditEventDto cancel(UUID id, TechnicalSession session) {
        if (session.role() != TechnicalRole.WRITE) {
            throw new MessagingForbiddenException("La sesión no tiene permiso para esta operación");
        }
        PendingEvent event = pendingEventRepository.findById(id).orElseThrow(() -> notFound(id));
        if (event.getStatus() == PendingEventStatus.CANCELLED) {
            throw new MessagingConflictException("El evento " + id + " ya está dado de baja");
        }
        event.cancel();
        return mapper.toDto(event);
    }

    private static MessagingNotFoundException notFound(UUID id) {
        return new MessagingNotFoundException("No existe el evento " + id);
    }

}
