package ar.edu.utn.frba.tps.monolith.tesoreria.service;

import ar.edu.utn.frba.tps.monolith.messaging.config.MessagingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Verifica por HTTP que el modulo de mensajeria responda: hace login en su API con la cuenta de solo
 * lectura de app.messaging, como Auditoria. Los timeouts del cliente estan en spring.http.client.
 */
@Service
public class MessagingProbe {

    private static final Logger log = LoggerFactory.getLogger(MessagingProbe.class);

    private final RestClient restClient;
    private final String username;
    private final String password;

    @Autowired
    public MessagingProbe(RestClient.Builder builder, MessagingProperties properties) {
        this.restClient = builder.baseUrl(properties.url()).build();
        this.username = properties.readUser();
        this.password = properties.readPassword();
    }

    /**
     * true si el login devuelve un token; false si el modulo no responde, rechaza la cuenta o falla de
     * cualquier otra forma. El diagnostico informa el resultado, asi que aca no se propaga ningun error.
     */
    public boolean canLogIn() {
        try {
            Session session = restClient.post()
                    .uri("/api/messaging/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new Credentials(username, password))
                    .retrieve()
                    .body(Session.class);
            return session != null && session.token() != null && !session.token().isBlank();
        } catch (RuntimeException e) {
            log.warn("El modulo de mensajeria no respondio al login del diagnostico: {}", e.getMessage());
            return false;
        }
    }

    private record Credentials(String username, String password) {
    }

    private record Session(String token) {
    }

}
