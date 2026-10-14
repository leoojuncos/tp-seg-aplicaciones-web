package ar.edu.utn.frba.tps.monolith.messaging.controller;

import ar.edu.utn.frba.tps.monolith.messaging.dto.AuditEventDto;
import ar.edu.utn.frba.tps.monolith.messaging.filter.TechnicalSessionFilter;
import ar.edu.utn.frba.tps.monolith.messaging.model.TechnicalSession;
import ar.edu.utn.frba.tps.monolith.messaging.service.PendingEventService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    /**
     * Da de baja el evento. El filtro {@link TechnicalSessionFilter} ya exige la sesion del modulo
     * para llegar hasta aca; el service exige ademas el rol WRITE.
     */
    @PostMapping("/{id}/cancel")
    public AuditEventDto cancel(@PathVariable UUID id, HttpServletRequest request) {
        TechnicalSession session = TechnicalSessionFilter.currentSession(request)
                .orElseThrow(() -> new IllegalStateException("El filtro no dejó la sesión en el request"));
        return pendingEventService.cancel(id, session);
    }

}
