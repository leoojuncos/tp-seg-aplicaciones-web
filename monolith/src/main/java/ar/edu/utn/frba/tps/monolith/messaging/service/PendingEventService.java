package ar.edu.utn.frba.tps.monolith.messaging.service;

import ar.edu.utn.frba.tps.monolith.messaging.dto.AuditEventDto;
import ar.edu.utn.frba.tps.monolith.messaging.exception.MessagingNotFoundException;
import ar.edu.utn.frba.tps.monolith.messaging.mapper.MessagingMapper;
import ar.edu.utn.frba.tps.monolith.messaging.repository.PendingEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
                .orElseThrow(() -> new MessagingNotFoundException("No existe el evento " + id));
    }

}
