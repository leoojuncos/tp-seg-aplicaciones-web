package ar.edu.utn.frba.tps.monolith.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuracion de app.messaging: la cola de eventos de auditoria, el vencimiento de las sesiones del
 * modulo y el acceso por HTTP a su API con la cuenta de solo lectura (url, readUser, readPassword), el
 * mismo que usa Auditoria.
 */
@ConfigurationProperties(prefix = "app.messaging")
public record MessagingProperties(String queue, Duration sessionTtl, String url, String readUser, String readPassword) {
}
