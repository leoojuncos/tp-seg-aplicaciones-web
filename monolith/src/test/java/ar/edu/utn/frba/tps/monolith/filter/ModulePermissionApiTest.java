package ar.edu.utn.frba.tps.monolith.filter;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Gating de modulos (TPS-15) contra la app real: las cookies se arman con el SessionCookieCodec real,
 * como en AuthApiTest. Con el permiso del modulo, el pedido pasa el filtro y llega a su endpoint.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ModulePermissionApiTest {

    private static final String COOKIE_NAME = "sgm_session";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionCookieCodec codec;

    private Cookie sessionWith(String... permissions) {
        return new Cookie(COOKIE_NAME, codec.encode(new SessionResponse("usuario", "ROL", List.of(permissions))));
    }

    @Test
    void moduleWithoutSessionIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/tesoreria/debts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    void moduleWithItsPermissionIsAllowed() throws Exception {
        mockMvc.perform(get("/api/tesoreria/debts").cookie(sessionWith("TESORERIA")))
                .andExpect(status().isOk());
    }

    @Test
    void moduleWithoutItsPermissionIsForbidden() throws Exception {
        mockMvc.perform(get("/api/tesoreria/debts").cookie(sessionWith("AUDITORIA", "ADMINISTRACION")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"));
    }

    @Test
    void actionsOfTheModuleAreGatedToo() throws Exception {
        mockMvc.perform(post("/api/tesoreria/debts/{id}/forgive", 1).cookie(sessionWith("AUDITORIA")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"));
    }

    @Test
    void sessionWithoutPermissionsIsForbiddenInEveryModule() throws Exception {
        mockMvc.perform(get("/api/tesoreria/debts").cookie(sessionWith()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/administracion/users").cookie(sessionWith()))
                .andExpect(status().isForbidden());
    }

    @Test
    void administracionRequiresItsOwnPermission() throws Exception {
        mockMvc.perform(get("/api/administracion/users").cookie(sessionWith("TESORERIA")))
                .andExpect(status().isForbidden());
        // Con el permiso pasa el filtro y llega al listado de usuarios.
        mockMvc.perform(get("/api/administracion/users").cookie(sessionWith("ADMINISTRACION")))
                .andExpect(status().isOk());
    }

    @Test
    void unknownModuleIsForbiddenEvenWithEveryPermission() throws Exception {
        Cookie everything = sessionWith("ADMINISTRACION", "TESORERIA", "AUDITORIA", "INGRESOS_PUBLICOS", "CONTADURIA");
        mockMvc.perform(get("/api/inexistente/cosas").cookie(everything))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"));
        mockMvc.perform(get("/api/").cookie(everything))
                .andExpect(status().isForbidden());
    }

    @Test
    void permissionMustMatchTheModuleExactly() throws Exception {
        // El permiso es el nombre del modulo en mayuscula, sin variantes.
        mockMvc.perform(get("/api/tesoreria/debts").cookie(sessionWith("tesoreria", "TESORERIA_LECTURA")))
                .andExpect(status().isForbidden());
    }

    @Test
    void authRoutesOnlyRequireASession() throws Exception {
        mockMvc.perform(get("/api/auth/session").cookie(sessionWith()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/logout").cookie(sessionWith()))
                .andExpect(status().isNoContent());
    }

    @Test
    void publicRoutesAreNotGated() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
        // VEP es publico: sin cookie, el pedido llega a su controller, que rechaza el CUIT mal formado con
        // 400; no se corta en 401/403.
        mockMvc.perform(get("/api/vep/debts/{cuit}", "123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation"));
    }

    @Test
    void dotSegmentsDoNotSkipTheGate() throws Exception {
        // La ruta se normaliza antes de elegir el modulo: entrar "por" auth no saltea el permiso.
        mockMvc.perform(get("/api/auth/../tesoreria/debts").cookie(sessionWith()))
                .andExpect(status().isForbidden());
    }

}
