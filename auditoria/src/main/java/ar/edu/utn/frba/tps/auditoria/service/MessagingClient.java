package ar.edu.utn.frba.tps.auditoria.service;

import ar.edu.utn.frba.tps.auditoria.config.MessagingProperties;
import ar.edu.utn.frba.tps.auditoria.dto.AuditEventMessage;
import ar.edu.utn.frba.tps.auditoria.exception.MessagingUnavailableException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;
import java.util.UUID;

/**
 * Cliente de la API del modulo de mensajeria del monolito. Se autentica con la cuenta de solo
 * lectura y consulta el almacen de pendientes, que es la fuente de verdad del estado del evento.
 */
@Service
public class MessagingClient {

    private final RestClient restClient;
    private final String username;
    private final String password;

    private String token;

    public MessagingClient(RestClient.Builder builder, MessagingProperties properties) {
        this.restClient = builder.baseUrl(properties.url()).build();
        this.username = properties.readUser();
        this.password = properties.readPassword();
    }

    /**
     * Busca el evento en el almacen de pendientes. Devuelve vacio solo ante el {@code 404
     * not_found} del contrato (el evento ya no esta). Si el token vencio (401), reautentica y
     * reintenta una vez. Ante cualquier respuesta no concluyente (modulo caido, error del
     * servidor, 404 ajeno al contrato, 200 sin cuerpo o token que sigue sin servir) lanza
     * {@link MessagingUnavailableException}: el evento no se confirma ni se descarta, para que el
     * consumo se reintente y no se pierda el registro.
     */
    public Optional<AuditEventMessage> findPending(UUID id) {
        try {
            return Optional.of(getPending(id));
        } catch (HttpClientErrorException.NotFound e) {
            return discardIfContractNotFound(e);
        } catch (HttpClientErrorException.Unauthorized e) {
            token = null;
            return retryAfterReauth(id);
        } catch (RestClientException e) {
            throw new MessagingUnavailableException("No se pudo consultar el modulo de mensajeria", e);
        }
    }

    private Optional<AuditEventMessage> retryAfterReauth(UUID id) {
        try {
            return Optional.of(getPending(id));
        } catch (HttpClientErrorException.NotFound e) {
            return discardIfContractNotFound(e);
        } catch (RestClientException e) {
            throw new MessagingUnavailableException("No se pudo autenticar contra el modulo de mensajeria", e);
        }
    }

    /**
     * Un 404 solo cuenta como baja del evento si trae el cuerpo de error del contrato
     * ({@code error: not_found}). Otro 404 (ruta inexistente, URL mal apuntada) no es concluyente:
     * se reintenta en vez de descartar en silencio.
     */
    private Optional<AuditEventMessage> discardIfContractNotFound(HttpClientErrorException.NotFound e) {
        ErrorBody body;
        try {
            body = e.getResponseBodyAs(ErrorBody.class);
        } catch (RestClientException parseError) {
            throw new MessagingUnavailableException("El modulo respondio 404 con un cuerpo ilegible", parseError);
        }
        if (body != null && "not_found".equals(body.error())) {
            return Optional.empty();
        }
        throw new MessagingUnavailableException("El modulo respondio 404 sin el cuerpo del contrato", e);
    }

    private AuditEventMessage getPending(UUID id) {
        AuditEventMessage event = restClient.get()
                .uri("/api/messaging/pending/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                .retrieve()
                .body(AuditEventMessage.class);
        if (event == null) {
            throw new MessagingUnavailableException("El modulo de mensajeria respondio sin cuerpo");
        }
        return event;
    }

    private synchronized String token() {
        if (token == null) {
            token = login();
        }
        return token;
    }

    private String login() {
        Session session = restClient.post()
                .uri("/api/messaging/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new Credentials(username, password))
                .retrieve()
                .body(Session.class);
        if (session == null || session.token() == null || session.token().isBlank()) {
            throw new MessagingUnavailableException("El modulo de mensajeria no devolvio un token de sesion");
        }
        return session.token();
    }

    private record Credentials(String username, String password) {
    }

    private record Session(String token) {
    }

    private record ErrorBody(String error, String message) {
    }

}
