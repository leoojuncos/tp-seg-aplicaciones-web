package ar.edu.utn.frba.tps.monolith.vep.controller;

import ar.edu.utn.frba.tps.monolith.vep.dto.VepDebtResponse;
import ar.edu.utn.frba.tps.monolith.vep.service.VepService;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta publica de la deuda de un CUIT. No se llama DebtController, como el de Tesoreria: Spring no
 * arranca con dos beans del mismo nombre (por lo mismo, el service es VepService).
 */
@RestController
@RequestMapping("/api/vep/debts")
public class VepController {

    private final VepService vepService;

    @Autowired
    public VepController(VepService vepService) {
        this.vepService = vepService;
    }

    // Sin cookie de sesion (ver ApiPaths). El CUIT va con sus 11 digitos, sin guiones.
    @GetMapping("/{cuit}")
    public VepDebtResponse get(@PathVariable @Pattern(regexp = "\\d{11}") String cuit) {
        return vepService.get(cuit);
    }

}
