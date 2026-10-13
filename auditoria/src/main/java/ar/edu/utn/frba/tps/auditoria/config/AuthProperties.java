package ar.edu.utn.frba.tps.auditoria.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Nombre de la cookie de sesion del SGM que lee el filtro de Auditoria. */
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(String cookieName) {
}
