package ar.edu.utn.frba.tps.auditoria.config;

import ar.edu.utn.frba.tps.auditoria.service.ConnectivityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ConnectivityStartupCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ConnectivityStartupCheck.class);

    private final ConnectivityService connectivityService;

    @Autowired
    public ConnectivityStartupCheck(ConnectivityService connectivityService) {
        this.connectivityService = connectivityService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info(connectivityService.checkDatabase() ? "DB OK" : "DB DOWN: no se pudo conectar a Postgres");
        log.info(connectivityService.checkRabbitMQ() ? "RabbitMQ OK" : "RabbitMQ DOWN: no se pudo conectar a RabbitMQ");
    }

}
