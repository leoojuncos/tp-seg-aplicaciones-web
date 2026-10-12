package ar.edu.utn.frba.tps.monolith.tesoreria.controller;

import ar.edu.utn.frba.tps.monolith.tesoreria.dto.DiagnosticsResponse;
import ar.edu.utn.frba.tps.monolith.tesoreria.service.DiagnosticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tesoreria/diagnostics")
public class DiagnosticsController {

    private final DiagnosticsService diagnosticsService;

    @Autowired
    public DiagnosticsController(DiagnosticsService diagnosticsService) {
        this.diagnosticsService = diagnosticsService;
    }

    // Siempre 200: es un informe del estado de las integraciones, no una sonda.
    @GetMapping
    public DiagnosticsResponse diagnose() {
        return diagnosticsService.diagnose();
    }

}
