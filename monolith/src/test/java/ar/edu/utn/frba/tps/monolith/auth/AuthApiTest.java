package ar.edu.utn.frba.tps.monolith.auth;

import ar.edu.utn.frba.tps.monolith.auth.dto.SessionResponse;
import ar.edu.utn.frba.tps.monolith.auth.service.SessionCookieCodec;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * No hay login real todavia (TPS-14): las cookies validas se arman con el {@link SessionCookieCodec}
 * real, inyectado, en vez de con un login.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthApiTest {

    private static final String COOKIE_NAME = "sgm_session";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionCookieCodec codec;

    @Test
    void sessionWithoutCookieIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/session"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    void sessionWithInvalidBase64IsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/session").cookie(new Cookie(COOKIE_NAME, "no-es-base64-valido!!")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    void sessionWithTheJsonLiteralNullIsUnauthorizedNotAServerError() throws Exception {
        // "null" es JSON valido: Jackson lo decodifica a null, no tira una excepcion. Con
        // Optional.of en vez de Optional.ofNullable esto era una NPE que escapaba como 500.
        String jsonNull = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("null".getBytes());

        mockMvc.perform(get("/api/auth/session").cookie(new Cookie(COOKIE_NAME, jsonNull)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    void sessionWithValidBase64ButMalformedJsonIsUnauthorized() throws Exception {
        // Base64 valido, pero lo que decodifica no es JSON en absoluto.
        String notJson = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("esto no es json".getBytes());

        mockMvc.perform(get("/api/auth/session").cookie(new Cookie(COOKIE_NAME, notJson)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    void sessionWithValidCookieIsReturnedAsIs() throws Exception {
        String cookieValue = codec.encode(new SessionResponse("admin", "ADMINISTRADOR", List.of("ADMINISTRACION")));

        mockMvc.perform(get("/api/auth/session").cookie(new Cookie(COOKIE_NAME, cookieValue)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMINISTRADOR"))
                .andExpect(jsonPath("$.permissions[0]").value("ADMINISTRACION"));
    }

    @Test
    void logoutWithValidCookieExpiresIt() throws Exception {
        String cookieValue = codec.encode(new SessionResponse("admin", "ADMINISTRADOR", List.of("ADMINISTRACION")));

        mockMvc.perform(post("/api/auth/logout").cookie(new Cookie(COOKIE_NAME, cookieValue)))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));
    }

    @Test
    void logoutWithoutCookieIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void healthStaysPublicWithoutASession() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    void messagingKeepsItsOwnSessionAndIsNotGatedByTheSgmCookie() throws Exception {
        // Si este filtro se colara sobre /api/messaging/**, el mensaje seria el propio
        // ("No hay sesión, o no es válida") en vez del de TechnicalSessionFilter.
        mockMvc.perform(get("/api/messaging/accounts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token ausente, desconocido o vencido"));
    }

    @Test
    void dotDotSegmentsCannotBypassTheFilterThroughAPublicPrefix() throws Exception {
        // Sin StringUtils.cleanPath, esta ruta empieza literalmente con "/api/messaging/" y
        // quedaria sin protección aunque en realidad apunte a /api/auth/session.
        mockMvc.perform(get("/api/messaging/../auth/session"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("No hay sesión, o no es válida"));
    }

}
