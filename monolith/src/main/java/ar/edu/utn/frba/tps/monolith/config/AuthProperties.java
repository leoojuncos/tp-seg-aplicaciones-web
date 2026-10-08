package ar.edu.utn.frba.tps.monolith.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(String cookieName) {
}
