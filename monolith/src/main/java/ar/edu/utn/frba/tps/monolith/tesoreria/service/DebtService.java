package ar.edu.utn.frba.tps.monolith.tesoreria.service;

import ar.edu.utn.frba.tps.monolith.exception.ConflictException;
import ar.edu.utn.frba.tps.monolith.exception.NotFoundException;
import ar.edu.utn.frba.tps.monolith.tesoreria.dto.DebtResponse;
import ar.edu.utn.frba.tps.monolith.tesoreria.mapper.TesoreriaMapper;
import ar.edu.utn.frba.tps.monolith.tesoreria.model.Debt;
import ar.edu.utn.frba.tps.monolith.tesoreria.model.DebtStatus;
import ar.edu.utn.frba.tps.monolith.tesoreria.repository.DebtRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DebtService {

    private final DebtRepository debtRepository;
    private final TesoreriaMapper mapper;

    @Autowired
    public DebtService(DebtRepository debtRepository, TesoreriaMapper mapper) {
        this.debtRepository = debtRepository;
        this.mapper = mapper;
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
     * Pasa la deuda a FORGIVEN y conserva su importe. Es transaccional para que lo que se sume a la
     * condonacion, como la publicacion del evento de auditoria, se revierta con ella.
     */
    @Transactional
    public DebtResponse forgive(Long id) {
        Debt debt = debtRepository.findWithLockById(id).orElseThrow(() -> notFound(id));
        if (debt.getStatus() == DebtStatus.FORGIVEN) {
            throw new ConflictException("La deuda " + id + " ya está condonada");
        }
        debt.forgive();
        return mapper.toResponse(debt);
    }

    private static NotFoundException notFound(Long id) {
        return new NotFoundException("No existe la deuda " + id);
    }

}
