package ar.edu.utn.frba.tps.monolith.tesoreria.mapper;

import ar.edu.utn.frba.tps.monolith.tesoreria.dto.DebtResponse;
import ar.edu.utn.frba.tps.monolith.tesoreria.model.Debt;
import org.springframework.stereotype.Component;

@Component
public class TesoreriaMapper {

    public DebtResponse toResponse(Debt debt) {
        return new DebtResponse(debt.getId(), debt.getCuit(), debt.getAmount(), debt.getStatus());
    }

}
