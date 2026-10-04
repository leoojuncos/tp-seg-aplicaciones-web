package ar.edu.utn.frba.tps.monolith.messaging.controller;

import ar.edu.utn.frba.tps.monolith.messaging.dto.LoginRequest;
import ar.edu.utn.frba.tps.monolith.messaging.dto.TechnicalSessionResponse;
import ar.edu.utn.frba.tps.monolith.messaging.service.TechnicalAccountService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messaging/auth")
public class TechnicalAuthController {

    private final TechnicalAccountService accountService;

    @Autowired
    public TechnicalAuthController(TechnicalAccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/login")
    public TechnicalSessionResponse login(@Valid @RequestBody LoginRequest request) {
        return accountService.login(request.username(), request.password());
    }

}
