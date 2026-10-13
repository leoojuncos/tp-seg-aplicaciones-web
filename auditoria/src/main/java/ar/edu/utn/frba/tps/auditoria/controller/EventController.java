package ar.edu.utn.frba.tps.auditoria.controller;

import ar.edu.utn.frba.tps.auditoria.dto.EventResponse;
import ar.edu.utn.frba.tps.auditoria.service.AuditEventService;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final AuditEventService service;

    public EventController(AuditEventService service) {
        this.service = service;
    }

    // Filtros opcionales, todos como query param: ?module=TESORERIA, ?cuit=20123456789 y el rango por
    // fecha de registro ?from=2026-10-01&to=2026-10-31.
    @GetMapping
    public List<EventResponse> list(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) @Pattern(regexp = "\\d{11}") String cuit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.list(module, cuit, from, to);
    }

    @GetMapping("/{id}")
    public EventResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

}
