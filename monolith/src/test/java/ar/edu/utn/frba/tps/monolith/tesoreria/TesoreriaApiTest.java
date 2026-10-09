package ar.edu.utn.frba.tps.monolith.tesoreria;

import ar.edu.utn.frba.tps.monolith.auth.dto.SessionResponse;
import ar.edu.utn.frba.tps.monolith.auth.service.SessionCookieCodec;
import ar.edu.utn.frba.tps.monolith.tesoreria.model.Debt;
import ar.edu.utn.frba.tps.monolith.tesoreria.model.DebtStatus;
import ar.edu.utn.frba.tps.monolith.tesoreria.repository.DebtRepository;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba la API de deudas contra Postgres, con el esquema de db/init/ aplicado. Cada test crea su
 * deuda, con un CUIT que el seed no usa, en una transaccion que se descarta al terminar. No hay login
 * todavia (TPS-14): la cookie de sesion se arma con el SessionCookieCodec real, como en AuthApiTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TesoreriaApiTest {

    private static final String CUIT = "33698765432";
    private static final String COOKIE_NAME = "sgm_session";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DebtRepository debtRepository;

    @Autowired
    private SessionCookieCodec codec;

    @Autowired
    private EntityManager entityManager;

    private Debt debt;
    private Cookie session;

    @BeforeEach
    void createDebtAndSession() {
        debt = debtRepository.saveAndFlush(new Debt(CUIT, new BigDecimal("1234.50")));
        session = new Cookie(COOKIE_NAME,
                codec.encode(new SessionResponse("tesorero", "TESORERO", List.of("TESORERIA"))));
    }

    @Test
    void debtsRequireTheSgmSession() throws Exception {
        mockMvc.perform(get("/api/tesoreria/debts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    void listFiltersByCuit() throws Exception {
        mockMvc.perform(get("/api/tesoreria/debts").param("cuit", CUIT).cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(debt.getId()))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void malformedCuitIsRejected() throws Exception {
        mockMvc.perform(get("/api/tesoreria/debts").param("cuit", "20-12345678-9").cookie(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation"));
    }

    @Test
    void forgivingKeepsTheAmount() throws Exception {
        mockMvc.perform(post("/api/tesoreria/debts/{id}/forgive", debt.getId()).cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FORGIVEN"))
                .andExpect(jsonPath("$.amount").value(1234.5));

        // Sin flush y clear, findById devolveria la entidad que esta en memoria y no lo que se escribio
        // en la base.
        entityManager.flush();
        entityManager.clear();
        assertThat(debtRepository.findById(debt.getId())).get()
                .extracting(Debt::getStatus).isEqualTo(DebtStatus.FORGIVEN);
    }

    @Test
    void forgivingTwiceIsAConflict() throws Exception {
        mockMvc.perform(post("/api/tesoreria/debts/{id}/forgive", debt.getId()).cookie(session))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/tesoreria/debts/{id}/forgive", debt.getId()).cookie(session))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("conflict"));
    }

    @Test
    void unknownDebtIsNotFound() throws Exception {
        mockMvc.perform(get("/api/tesoreria/debts/{id}", -1).cookie(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));

        mockMvc.perform(post("/api/tesoreria/debts/{id}/forgive", -1).cookie(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

}
