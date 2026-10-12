package ar.edu.utn.frba.tps.auditoria.controller;

import ar.edu.utn.frba.tps.auditoria.dto.Session;
import ar.edu.utn.frba.tps.auditoria.model.AuditEvent;
import ar.edu.utn.frba.tps.auditoria.repository.AuditEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba la API de eventos de Auditoria contra Postgres (tabla audit_events de db/init/). Cada test
 * crea su evento en una transaccion que se descarta al terminar. La cookie de sesion del SGM se arma
 * con el mismo formato que emite el monolito: base64 url-safe, sin padding, del JSON de la sesion.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuditoriaEventsApiTest {

    private static final String COOKIE_NAME = "sgm_session";
    private static final Instant OCCURRED = Instant.parse("2026-10-04T21:15:30.123456Z");
    private static final Instant REGISTERED = Instant.parse("2026-10-04T21:18:30.123456Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditEventRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    private AuditEvent event;
    private Cookie session;

    @BeforeEach
    void createEventAndSession() throws Exception {
        event = repository.saveAndFlush(
                new AuditEvent(UUID.randomUUID(), "DEBT_FORGIVEN", "20123456789", OCCURRED, REGISTERED));
        session = cookie(new Session("auditor", "AUDITOR", List.of("AUDITORIA")));
    }

    @Test
    void eventsRequireTheSgmSession() throws Exception {
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    void eventsRequireTheAuditoriaPermission() throws Exception {
        Cookie withoutPermission = cookie(new Session("tesorero", "TESORERO", List.of("TESORERIA")));

        mockMvc.perform(get("/api/events").cookie(withoutPermission))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"));
    }

    @Test
    void healthStaysPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    void listReturnsTheRegisteredEvent() throws Exception {
        mockMvc.perform(get("/api/events").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + event.getId() + "')]").exists())
                .andExpect(jsonPath("$[?(@.id == '" + event.getId() + "')].cuit").value("20123456789"));
    }

    @Test
    void listFilteredByItsModuleReturnsTheEvent() throws Exception {
        mockMvc.perform(get("/api/events").param("module", "TESORERIA").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + event.getId() + "')]").exists());
    }

    @Test
    void listFilteredByAnotherModuleLeavesItOut() throws Exception {
        mockMvc.perform(get("/api/events").param("module", "ADMINISTRACION").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + event.getId() + "')]").doesNotExist());
    }

    @Test
    void listFilteredByItsCuitReturnsTheEvent() throws Exception {
        mockMvc.perform(get("/api/events").param("cuit", "20123456789").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + event.getId() + "')]").exists());
    }

    @Test
    void listFilteredByAnotherCuitLeavesItOut() throws Exception {
        mockMvc.perform(get("/api/events").param("cuit", "99999999999").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + event.getId() + "')]").doesNotExist());
    }

    @Test
    void listWithinTheRegistrationDateRangeReturnsTheEvent() throws Exception {
        mockMvc.perform(get("/api/events").param("from", "2026-10-04").param("to", "2026-10-04").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + event.getId() + "')]").exists());
    }

    @Test
    void listOutsideTheRegistrationDateRangeLeavesItOut() throws Exception {
        mockMvc.perform(get("/api/events").param("from", "2026-10-05").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + event.getId() + "')]").doesNotExist());
    }

    @Test
    void theDateRangeUsesTheArgentineDay() throws Exception {
        // 01:00 UTC del 5/10 son las 22:00 del 4/10 en Argentina: entra en el rango del 4/10.
        AuditEvent lateNight = repository.saveAndFlush(new AuditEvent(
                UUID.randomUUID(), "DEBT_FORGIVEN", "20123456789", OCCURRED, Instant.parse("2026-10-05T01:00:00Z")));

        mockMvc.perform(get("/api/events").param("from", "2026-10-04").param("to", "2026-10-04").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + lateNight.getId() + "')]").exists());
    }

    @Test
    void rejectsAnInvalidCuit() throws Exception {
        mockMvc.perform(get("/api/events").param("cuit", "no-es-un-cuit").cookie(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation"));
    }

    @Test
    void detailReturnsTheEvent() throws Exception {
        mockMvc.perform(get("/api/events/{id}", event.getId()).cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(event.getId().toString()))
                .andExpect(jsonPath("$.type").value("DEBT_FORGIVEN"))
                .andExpect(jsonPath("$.cuit").value("20123456789"));
    }

    @Test
    void unknownEventIsNotFound() throws Exception {
        mockMvc.perform(get("/api/events/{id}", UUID.randomUUID()).cookie(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    private Cookie cookie(Session value) throws Exception {
        String encoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(value));
        return new Cookie(COOKIE_NAME, encoded);
    }

}
