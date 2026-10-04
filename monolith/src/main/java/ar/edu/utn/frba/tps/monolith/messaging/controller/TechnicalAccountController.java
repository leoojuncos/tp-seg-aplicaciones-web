package ar.edu.utn.frba.tps.monolith.messaging.controller;

import ar.edu.utn.frba.tps.monolith.messaging.dto.TechnicalAccountResponse;
import ar.edu.utn.frba.tps.monolith.messaging.service.TechnicalAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/messaging/accounts")
public class TechnicalAccountController {

    private final TechnicalAccountService accountService;

    @Autowired
    public TechnicalAccountController(TechnicalAccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public List<TechnicalAccountResponse> list() {
        return accountService.listAccounts();
    }

}
