package ar.edu.utn.frba.tps.monolith.tesoreria.service;

import ar.edu.utn.frba.tps.monolith.exception.ConflictException;
import ar.edu.utn.frba.tps.monolith.exception.NotFoundException;
import ar.edu.utn.frba.tps.monolith.messaging.service.AuditEventPublisher;
import ar.edu.utn.frba.tps.monolith.tesoreria.dto.DebtResponse;
import ar.edu.utn.frba.tps.monolith.tesoreria.mapper.TesoreriaMapper;
import ar.edu.utn.frba.tps.monolith.tesoreria.model.Debt;
import ar.edu.utn.frba.tps.monolith.tesoreria.model.DebtStatus;
import ar.edu.utn.frba.tps.monolith.tesoreria.repository.DebtRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class DebtService {

    private final DebtRepository debtRepository;
    private final TesoreriaMapper mapper;
    private final AuditEventPublisher auditEventPublisher;

    @Autowired
    public DebtService(DebtRepository debtRepository, TesoreriaMapper mapper, AuditEventPublisher auditEventPublisher) {
        this.debtRepository = debtRepository;
        this.mapper = mapper;
        this.auditEventPublisher = auditEventPublisher;
    }

    /** Todas las deudas, ordenadas por CUIT, o solo la del CUIT pedido. */
    public List<DebtResponse> list(String cuit) {
        List<Debt> debts = cuit == null
                ? debtRepository.findAll(Sort.by("cuit"))
                : debtRepository.findByCuit(cuit);
        return debts.stream()
                .map(mapper::toResponse)
                .toList();
    }

    public DebtResponse get(Long id) {
        return debtRepository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> notFound(id));
    }

    /**
     * Pasa la deuda a FORGIVEN, conserva su importe y le entrega el evento de auditoria al modulo
     * messaging, en la misma transaccion: si el evento no se puede encolar, la excepcion revierte la
     * condonacion. No hay condonacion sin evento.
     */
    @Transactional
    public DebtResponse forgive(Long id) {
        Debt debt = debtRepository.findWithLockById(id).orElseThrow(() -> notFound(id));
        if (debt.getStatus() == DebtStatus.FORGIVEN) {
            throw new ConflictException("La deuda " + id + " ya está condonada");
        }
        debt.forgive();
        auditEventPublisher.publish(AuditEventPublisher.DEBT_FORGIVEN, debt.getCuit(), Instant.now());
        return mapper.toResponse(debt);
    }

    private static NotFoundException notFound(Long id) {
        return new NotFoundException("No existe la deuda " + id);
    }

}
