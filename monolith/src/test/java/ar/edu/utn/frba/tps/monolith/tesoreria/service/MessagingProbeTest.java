package ar.edu.utn.frba.tps.monolith.tesoreria.service;

import ar.edu.utn.frba.tps.monolith.messaging.config.MessagingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.ConnectException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Prueba la sonda del modulo de mensajeria contra un servidor HTTP simulado, como MessagingClientTest de Auditoria. */
class MessagingProbeTest {

    private static final String BASE_URL = "http://monolith:8080";
    private static final String LOGIN = BASE_URL + "/api/messaging/auth/login";

    private MockRestServiceServer server;
    private MessagingProbe probe;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        probe = new MessagingProbe(builder, new MessagingProperties(
                "auditoria.events", Duration.ofMinutes(30), BASE_URL, "auditoria_lector", "Auditoria2026!"));
    }

    @Test
    void isUpWhenTheLoginReturnsAToken() {
        server.expect(requestTo(LOGIN)).andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"username\":\"auditoria_lector\",\"password\":\"Auditoria2026!\"}"))
                .andRespond(withSuccess("{\"token\":\"tok-123\",\"username\":\"auditoria_lector\",\"role\":\"READ\"}",
                        MediaType.APPLICATION_JSON));

        assertThat(probe.canLogIn()).isTrue();
        server.verify();
    }

    @Test
    void isDownWhenTheAccountIsRejected() {
        server.expect(requestTo(LOGIN))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .body("{\"error\":\"unauthorized\",\"message\":\"Credenciales invalidas\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThat(probe.canLogIn()).isFalse();
        server.verify();
    }

    @Test
    void isDownWhenTheModuleDoesNotAnswer() {
        server.expect(requestTo(LOGIN)).andRespond(request -> {
            throw new ConnectException("Connection refused");
        });

        assertThat(probe.canLogIn()).isFalse();
        server.verify();
    }

    @Test
    void isDownWhenTheLoginReturnsNoToken() {
        server.expect(requestTo(LOGIN)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThat(probe.canLogIn()).isFalse();
        server.verify();
    }

}
