package ar.edu.utn.frba.tps.monolith.vep.mapper;

import ar.edu.utn.frba.tps.monolith.tesoreria.model.Debt;
import ar.edu.utn.frba.tps.monolith.vep.dto.VepDebtResponse;
import ar.edu.utn.frba.tps.monolith.vep.dto.VepTaxpayerResponse;
import ar.edu.utn.frba.tps.monolith.vep.model.Taxpayer;
import org.springframework.stereotype.Component;

@Component
public class VepMapper {

    public VepDebtResponse toResponse(Debt debt) {
        return new VepDebtResponse(debt.getCuit(), debt.getAmount(), debt.getStatus());
    }

    public VepTaxpayerResponse toResponse(Taxpayer taxpayer) {
        return new VepTaxpayerResponse(taxpayer.getCuit(), taxpayer.getName());
    }

}
