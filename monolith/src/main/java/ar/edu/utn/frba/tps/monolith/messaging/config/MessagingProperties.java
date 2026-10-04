package ar.edu.utn.frba.tps.monolith.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.messaging")
public record MessagingProperties(Duration sessionTtl) {
}
