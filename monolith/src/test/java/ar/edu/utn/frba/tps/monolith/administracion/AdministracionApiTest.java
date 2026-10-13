package ar.edu.utn.frba.tps.monolith.administracion;

import ar.edu.utn.frba.tps.monolith.auth.dto.SessionResponse;
import ar.edu.utn.frba.tps.monolith.auth.model.User;
import ar.edu.utn.frba.tps.monolith.auth.repository.UserRepository;
import ar.edu.utn.frba.tps.monolith.auth.service.SessionCookieCodec;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba la API de Administracion contra Postgres, con el seed de db/init/ (usuarios soporte,
 * tesorero, auditor, admin y operador). La cookie de sesion se arma con el SessionCookieCodec real,
 * como en TesoreriaApiTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdministracionApiTest {

    private static final String COOKIE_NAME = "sgm_session";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionCookieCodec codec;

    private Cookie adminSession;

    @BeforeEach
    void createSession() {
        adminSession = new Cookie(COOKIE_NAME,
                codec.encode(new SessionResponse("admin", "ADMINISTRADOR", List.of("ADMINISTRACION"))));
    }

    @Test
    void usersRequireTheSgmSession() throws Exception {
        mockMvc.perform(get("/api/administracion/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    void usersRequireTheAdministracionPermission() throws Exception {
        Cookie withoutPermission = new Cookie(COOKIE_NAME,
                codec.encode(new SessionResponse("tesorero", "TESORERO", List.of("TESORERIA"))));

        mockMvc.perform(get("/api/administracion/users").cookie(withoutPermission))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"));
    }

    @Test
    void listReturnsEverySeedUser() throws Exception {
        int count = (int) userRepository.count();

        mockMvc.perform(get("/api/administracion/users").cookie(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(count))
                .andExpect(jsonPath("$[0].username").value("admin"))
                .andExpect(jsonPath("$[0].role").value("ADMINISTRADOR"));
    }

    @Test
    void detailShowsTheRoleAndThePermissionCodes() throws Exception {
        User admin = user("admin");

        mockMvc.perform(get("/api/administracion/users/{id}", admin.getId()).cookie(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMINISTRADOR"))
                .andExpect(jsonPath("$.permissions", hasItems("ADMINISTRACION", "AUDITORIA", "TESORERIA")));
    }

    @Test
    void unknownUserIsNotFound() throws Exception {
        mockMvc.perform(get("/api/administracion/users/{id}", -1).cookie(adminSession))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    private User user(String username) {
        return userRepository.findAll().stream()
                .filter(candidate -> candidate.getUsername().equals(username))
                .findFirst()
                .orElseThrow();
    }

}
