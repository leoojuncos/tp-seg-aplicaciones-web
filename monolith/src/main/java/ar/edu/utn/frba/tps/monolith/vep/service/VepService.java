package ar.edu.utn.frba.tps.monolith.vep.service;

import ar.edu.utn.frba.tps.monolith.exception.NotFoundException;
import ar.edu.utn.frba.tps.monolith.tesoreria.repository.DebtRepository;
import ar.edu.utn.frba.tps.monolith.vep.dto.VepDebtResponse;
import ar.edu.utn.frba.tps.monolith.vep.mapper.VepMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VepService {

    private final DebtRepository debtRepository;
    private final VepMapper mapper;

    @Autowired
    public VepService(DebtRepository debtRepository, VepMapper mapper) {
        this.debtRepository = debtRepository;
        this.mapper = mapper;
    }

    /**
     * La deuda del CUIT con su estado actual: una condonada conserva su importe original. Hay una deuda
     * por CUIT (UNIQUE en debts), asi que findByCuit trae una o ninguna.
     */
    public VepDebtResponse get(String cuit) {
        return debtRepository.findByCuit(cuit).stream()
                .findFirst()
                .map(mapper::toResponse)
                .orElseThrow(() -> new NotFoundException("No hay deuda para el CUIT " + cuit));
    }

}
