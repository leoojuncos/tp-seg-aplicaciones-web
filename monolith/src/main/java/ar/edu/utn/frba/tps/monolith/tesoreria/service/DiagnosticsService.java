package ar.edu.utn.frba.tps.monolith.tesoreria.service;

import ar.edu.utn.frba.tps.monolith.messaging.config.MessagingProperties;
import ar.edu.utn.frba.tps.monolith.service.ConnectivityService;
import ar.edu.utn.frba.tps.monolith.tesoreria.dto.DiagnosticsResponse;
import ar.edu.utn.frba.tps.monolith.tesoreria.dto.DiagnosticsResponse.Integration;
import ar.edu.utn.frba.tps.monolith.tesoreria.dto.DiagnosticsResponse.MessagingIntegration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Diagnostico de integraciones de Tesoreria: la base y RabbitMQ, con el mismo chequeo que el health, y el
 * modulo de mensajeria, con un login HTTP con su cuenta de solo lectura.
 *
 * <p>La respuesta incluye la URL del modulo y esa credencial, codificada en Base64 (no cifrada): es la
 * vulnerabilidad #3 del escenario (ver AGENTS.md) y no se corrige aca. No se suma ninguna otra configuracion.
 */
@Service
public class DiagnosticsService {

    private final ConnectivityService connectivityService;
    private final MessagingProbe messagingProbe;
    private final MessagingProperties messagingProperties;

    @Autowired
    public DiagnosticsService(ConnectivityService connectivityService, MessagingProbe messagingProbe,
                              MessagingProperties messagingProperties) {
        this.connectivityService = connectivityService;
        this.messagingProbe = messagingProbe;
        this.messagingProperties = messagingProperties;
    }

    public DiagnosticsResponse diagnose() {
        return new DiagnosticsResponse(
                new Integration(status(connectivityService.checkDatabase())),
                new Integration(status(connectivityService.checkRabbitMQ())),
                new MessagingIntegration(status(messagingProbe.canLogIn()), messagingProperties.url(),
                        messagingProperties.readUser(), base64(messagingProperties.readPassword())));
    }

    private static String status(boolean up) {
        return up ? "UP" : "DOWN";
    }

    private static String base64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

}
