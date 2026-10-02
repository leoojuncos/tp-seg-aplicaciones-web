package ar.edu.utn.frba.tps.auditoria.controller;

import ar.edu.utn.frba.tps.auditoria.service.ConnectivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final ConnectivityService connectivityService;

    @Autowired
    public HealthController(ConnectivityService connectivityService) {
        this.connectivityService = connectivityService;
    }

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, String>> health() {
        boolean dbUp = connectivityService.checkDatabase();
        boolean rabbitUp = connectivityService.checkRabbitMQ();

        Map<String, String> body = new LinkedHashMap<>();
        body.put("status", dbUp && rabbitUp ? "UP" : "DOWN");
        body.put("db", dbUp ? "UP" : "DOWN");
        body.put("rabbitmq", rabbitUp ? "UP" : "DOWN");

        HttpStatus httpStatus = dbUp && rabbitUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus).body(body);
    }

}
