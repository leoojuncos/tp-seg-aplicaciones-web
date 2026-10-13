package ar.edu.utn.frba.tps.auditoria.mapper;

import ar.edu.utn.frba.tps.auditoria.dto.EventResponse;
import ar.edu.utn.frba.tps.auditoria.model.AuditEvent;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {

    public EventResponse toResponse(AuditEvent event) {
        return new EventResponse(event.getId(), event.getType(), event.getCuit(),
                event.getOccurredAt(), event.getRegisteredAt());
    }

}
