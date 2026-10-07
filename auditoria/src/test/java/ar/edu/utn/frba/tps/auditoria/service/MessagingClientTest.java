package ar.edu.utn.frba.tps.auditoria.service;

import ar.edu.utn.frba.tps.auditoria.config.MessagingProperties;
import ar.edu.utn.frba.tps.auditoria.dto.AuditEventMessage;
import ar.edu.utn.frba.tps.auditoria.exception.MessagingUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MessagingClientTest {

    private static final String BASE_URL = "http://localhost:8080";
    private static final String LOGIN = BASE_URL + "/api/messaging/auth/login";

    private MockRestServiceServer server;
    private MessagingClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new MessagingClient(builder, new MessagingProperties(BASE_URL, "auditoria_lector", "Auditoria2026!"));
    }

    @Test
    void authenticatesAndReturnsThePendingEvent() {
        UUID id = UUID.randomUUID();
        server.expect(requestTo(LOGIN)).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"token\":\"tok-123\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/messaging/pending/" + id)).andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer tok-123"))
                .andRespond(withSuccess(eventJson(id, "PENDING"), MediaType.APPLICATION_JSON));

        Optional<AuditEventMessage> result = client.findPending(id);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(id);
        assertThat(result.get().status()).isEqualTo("PENDING");
        assertThat(result.get().cuit()).isEqualTo("20123456789");
        server.verify();
    }

    @Test
    void returnsEmptyWhenTheEventIsNotFound() {
        UUID id = UUID.randomUUID();
        server.expect(requestTo(LOGIN)).andRespond(withSuccess("{\"token\":\"tok-123\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/messaging/pending/" + id))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .body("{\"error\":\"not_found\",\"message\":\"no existe\"}").contentType(MediaType.APPLICATION_JSON));

        Optional<AuditEventMessage> result = client.findPending(id);

        assertThat(result).isEmpty();
        server.verify();
    }

    @Test
    void throwsWhenTheNotFoundIsNotFromTheContract() {
        UUID id = UUID.randomUUID();
        server.expect(requestTo(LOGIN)).andRespond(withSuccess("{\"token\":\"tok-123\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/messaging/pending/" + id))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .body("{\"status\":404,\"error\":\"Not Found\"}").contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.findPending(id)).isInstanceOf(MessagingUnavailableException.class);
        server.verify();
    }

    @Test
    void throwsWhenTheModuleRespondsWithoutBody() {
        UUID id = UUID.randomUUID();
        server.expect(requestTo(LOGIN)).andRespond(withSuccess("{\"token\":\"tok-123\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/messaging/pending/" + id)).andRespond(withStatus(HttpStatus.OK));

        assertThatThrownBy(() -> client.findPending(id)).isInstanceOf(MessagingUnavailableException.class);
        server.verify();
    }

    @Test
    void reauthenticatesWhenTheTokenExpired() {
        UUID id = UUID.randomUUID();
        server.expect(requestTo(LOGIN)).andRespond(withSuccess("{\"token\":\"vencido\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/messaging/pending/" + id))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer vencido"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        server.expect(requestTo(LOGIN)).andRespond(withSuccess("{\"token\":\"nuevo\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/messaging/pending/" + id))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer nuevo"))
                .andRespond(withSuccess(eventJson(id, "PENDING"), MediaType.APPLICATION_JSON));

        Optional<AuditEventMessage> result = client.findPending(id);

        assertThat(result).isPresent();
        server.verify();
    }

    @Test
    void throwsWhenTheModuleReturnsAServerError() {
        UUID id = UUID.randomUUID();
        server.expect(requestTo(LOGIN)).andRespond(withSuccess("{\"token\":\"tok-123\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/messaging/pending/" + id))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.findPending(id)).isInstanceOf(MessagingUnavailableException.class);
        server.verify();
    }

    @Test
    void throwsWhenItCannotAuthenticate() {
        UUID id = UUID.randomUUID();
        server.expect(requestTo(LOGIN)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        server.expect(requestTo(LOGIN)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.findPending(id)).isInstanceOf(MessagingUnavailableException.class);
        server.verify();
    }

    private String eventJson(UUID id, String status) {
        return "{\"id\":\"" + id + "\",\"type\":\"DEBT_FORGIVEN\",\"cuit\":\"20123456789\","
                + "\"timestamp\":\"2026-10-04T21:15:30.123456Z\",\"status\":\"" + status + "\"}";
    }

}
