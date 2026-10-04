package ar.edu.utn.frba.tps.monolith.messaging.controller;

import ar.edu.utn.frba.tps.monolith.messaging.dto.QueueStatusResponse;
import ar.edu.utn.frba.tps.monolith.messaging.service.QueueStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messaging/queue")
public class QueueStatusController {

    private final QueueStatusService queueStatusService;

    @Autowired
    public QueueStatusController(QueueStatusService queueStatusService) {
        this.queueStatusService = queueStatusService;
    }

    @GetMapping
    public QueueStatusResponse status() {
        return queueStatusService.status();
    }

}
