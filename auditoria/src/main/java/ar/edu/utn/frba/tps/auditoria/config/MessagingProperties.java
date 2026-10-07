package ar.edu.utn.frba.tps.auditoria.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Datos de acceso a la API del modulo de mensajeria del monolito. */
@ConfigurationProperties(prefix = "app.messaging")
public record MessagingProperties(String url, String readUser, String readPassword) {
}
