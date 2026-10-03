package ar.edu.utn.frba.tps.auditoria.config;

import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    /**
     * Declara la cola de eventos de auditoria. Al existir este bean, el RabbitAdmin que
     * autoconfigura Spring Boot la crea en el broker al arrancar, de modo que el micro
     * queda escuchando aunque todavia nadie publique.
     *
     * <p>Durable para que la cola (y sus mensajes) sobrevivan a un reinicio del broker.
     * El nombre es provisional (ver application.yml); el contrato definitivo lo fijan
     * TPS-18 (producer en Tesoreria) y TPS-22 (consumer real).
     */
    @Bean
    public Queue auditoriaQueue(@Value("${app.rabbitmq.queue}") String queueName) {
        return new Queue(queueName, true);
    }

}
