package ar.edu.utn.frba.tps.monolith.vep.mapper;

import ar.edu.utn.frba.tps.monolith.tesoreria.model.Debt;
import ar.edu.utn.frba.tps.monolith.vep.dto.VepDebtResponse;
import org.springframework.stereotype.Component;

@Component
public class VepMapper {

    public VepDebtResponse toResponse(Debt debt) {
        return new VepDebtResponse(debt.getCuit(), debt.getAmount(), debt.getStatus());
    }

}
