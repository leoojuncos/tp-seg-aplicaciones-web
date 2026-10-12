package ar.edu.utn.frba.tps.monolith.tesoreria;

import ar.edu.utn.frba.tps.monolith.auth.dto.SessionResponse;
import ar.edu.utn.frba.tps.monolith.auth.service.SessionCookieCodec;
import ar.edu.utn.frba.tps.monolith.messaging.config.MessagingProperties;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import ar.edu.utn.frba.tps.monolith.tesoreria.service.MessagingProbe;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba el Diagnostico de integraciones contra Postgres y RabbitMQ reales. La sonda HTTP del modulo de
 * mensajeria se reemplaza por un mock: su comportamiento lo cubre MessagingProbeTest. La cookie de sesion
 * se arma con el SessionCookieCodec real, como en TesoreriaApiTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DiagnosticsApiTest {

    private static final String COOKIE_NAME = "sgm_session";
    private static final String PATH = "/api/tesoreria/diagnostics";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionCookieCodec codec;

    @Autowired
    private MessagingProperties properties;

    @MockitoBean
    private MessagingProbe messagingProbe;

    private Cookie session() {
        return new Cookie(COOKIE_NAME, codec.encode(new SessionResponse("tesorero", "TESORERO", List.of("TESORERIA"))));
    }

    @Test
    void reportsTheIntegrationsAndTheMessagingConfiguration() throws Exception {
        when(messagingProbe.canLogIn()).thenReturn(true);

        // Exactamente estos campos: la respuesta no suma otra configuracion que la del modulo.
        mockMvc.perform(get(PATH).cookie(session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(3)))
                .andExpect(jsonPath("$.database.*", hasSize(1)))
                .andExpect(jsonPath("$.database.status").value("UP"))
                .andExpect(jsonPath("$.rabbitmq.*", hasSize(1)))
                .andExpect(jsonPath("$.rabbitmq.status").value("UP"))
                .andExpect(jsonPath("$.messaging.*", hasSize(4)))
                .andExpect(jsonPath("$.messaging.status").value("UP"))
                .andExpect(jsonPath("$.messaging.url").value(properties.url()))
                .andExpect(jsonPath("$.messaging.user").value(properties.readUser()))
                .andExpect(jsonPath("$.messaging.password")
                        .value(Base64.getEncoder().encodeToString(properties.readPassword().getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void messagingIsDownWhenTheProbeFails() throws Exception {
        when(messagingProbe.canLogIn()).thenReturn(false);

        mockMvc.perform(get(PATH).cookie(session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messaging.status").value("DOWN"))
                .andExpect(jsonPath("$.messaging.url").value(properties.url()));
    }

    @Test
    void diagnosticsRequireTheSgmSession() throws Exception {
        mockMvc.perform(get(PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
        verifyNoInteractions(messagingProbe);
    }

}
