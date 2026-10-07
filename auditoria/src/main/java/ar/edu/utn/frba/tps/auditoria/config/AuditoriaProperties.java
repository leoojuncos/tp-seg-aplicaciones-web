package ar.edu.utn.frba.tps.auditoria.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Parametros del consumo de eventos. */
@ConfigurationProperties(prefix = "app.auditoria")
public record AuditoriaProperties(Duration delay) {
}
