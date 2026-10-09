package ar.edu.utn.frba.tps.monolith.tesoreria.controller;

import ar.edu.utn.frba.tps.monolith.tesoreria.dto.DebtResponse;
import ar.edu.utn.frba.tps.monolith.tesoreria.service.DebtService;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tesoreria/debts")
public class DebtController {

    private final DebtService debtService;

    @Autowired
    public DebtController(DebtService debtService) {
        this.debtService = debtService;
    }

    // El filtro va con los 11 digitos del CUIT, sin guiones.
    @GetMapping
    public List<DebtResponse> list(@RequestParam(required = false) @Pattern(regexp = "\\d{11}") String cuit) {
        return debtService.list(cuit);
    }

    @GetMapping("/{id}")
    public DebtResponse get(@PathVariable Long id) {
        return debtService.get(id);
    }

    @PostMapping("/{id}/forgive")
    public DebtResponse forgive(@PathVariable Long id) {
        return debtService.forgive(id);
    }

}
