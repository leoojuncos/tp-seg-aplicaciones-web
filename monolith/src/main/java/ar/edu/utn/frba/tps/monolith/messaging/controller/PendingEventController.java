package ar.edu.utn.frba.tps.monolith.messaging.controller;

import ar.edu.utn.frba.tps.monolith.messaging.dto.AuditEventDto;
import ar.edu.utn.frba.tps.monolith.messaging.service.PendingEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/messaging/pending")
public class PendingEventController {

    private final PendingEventService pendingEventService;

    @Autowired
    public PendingEventController(PendingEventService pendingEventService) {
        this.pendingEventService = pendingEventService;
    }

    @GetMapping
    public List<AuditEventDto> list() {
        return pendingEventService.list();
    }

    @GetMapping("/{id}")
    public AuditEventDto get(@PathVariable UUID id) {
        return pendingEventService.get(id);
    }

}
