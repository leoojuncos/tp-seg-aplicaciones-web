package ar.edu.utn.frba.tps.monolith.vep.controller;

import ar.edu.utn.frba.tps.monolith.vep.dto.VepLoginRequest;
import ar.edu.utn.frba.tps.monolith.vep.dto.VepTaxpayerResponse;
import ar.edu.utn.frba.tps.monolith.vep.service.VepService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Ingreso al portal del contribuyente. Sin cookie de sesion, como el resto de /api/vep/ (ver ApiPaths). */
@RestController
@RequestMapping("/api/vep/auth")
public class VepAuthController {

    private final VepService vepService;

    @Autowired
    public VepAuthController(VepService vepService) {
        this.vepService = vepService;
    }

    @PostMapping("/login")
    public VepTaxpayerResponse login(@Valid @RequestBody VepLoginRequest request) {
        return vepService.login(request.cuit(), request.password());
    }

}
