package ar.edu.utn.frba.tps.auditoria.service;

import ar.edu.utn.frba.tps.auditoria.dto.EventResponse;
import ar.edu.utn.frba.tps.auditoria.exception.NotFoundException;
import ar.edu.utn.frba.tps.auditoria.mapper.EventMapper;
import ar.edu.utn.frba.tps.auditoria.model.AuditEvent;
import ar.edu.utn.frba.tps.auditoria.repository.AuditEventRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Consulta de los eventos de auditoria registrados. */
@Service
public class AuditEventService {

    // A que modulo del SGM pertenece cada tipo de evento. Hoy solo la condonacion, que es de Tesoreria.
    private static final Map<String, String> MODULE_BY_TYPE = Map.of("DEBT_FORGIVEN", "TESORERIA");

    // El dia de registro se toma en hora argentina, la misma en que el front muestra las fechas: asi el
    // rango coincide con la columna Registrado. La JVM corre en UTC, por eso la zona va explicita.
    private static final ZoneId SGM_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    private final AuditEventRepository repository;
    private final EventMapper mapper;

    public AuditEventService(AuditEventRepository repository, EventMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * Los eventos registrados, del mas reciente al mas antiguo. Cada filtro que viene con valor acota el
     * resultado: el modulo, el CUIT exacto y el rango por dia de registro (inclusive).
     */
    public List<EventResponse> list(String module, String cuit, LocalDate from, LocalDate to) {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "registeredAt")).stream()
                .filter(event -> module == null || module.equals(MODULE_BY_TYPE.get(event.getType())))
                .filter(event -> cuit == null || cuit.equals(event.getCuit()))
                .filter(event -> from == null || !registrationDate(event).isBefore(from))
                .filter(event -> to == null || !registrationDate(event).isAfter(to))
                .map(mapper::toResponse)
                .toList();
    }

    private static LocalDate registrationDate(AuditEvent event) {
        return event.getRegisteredAt().atZone(SGM_ZONE).toLocalDate();
    }

    public EventResponse get(UUID id) {
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new NotFoundException("No existe el evento " + id));
    }

}
