package ar.edu.utn.frba.tps.monolith.vep;

import ar.edu.utn.frba.tps.monolith.tesoreria.model.Debt;
import ar.edu.utn.frba.tps.monolith.tesoreria.repository.DebtRepository;
import ar.edu.utn.frba.tps.monolith.vep.dto.VepLoginRequest;
import ar.edu.utn.frba.tps.monolith.vep.model.Taxpayer;
import ar.edu.utn.frba.tps.monolith.vep.repository.TaxpayerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba la consulta publica del VEP y el ingreso al portal contra Postgres, con el esquema de db/init/
 * aplicado. Cada test crea su deuda y su contribuyente, con un CUIT que el seed no usa, en una transaccion
 * que se descarta al terminar. Ningun pedido lleva la cookie de sesion: /api/vep/** es publico. La deuda se condona sobre la entidad, sin pasar por
 * Tesoreria, para no depender de RabbitMQ; la condonacion en si la prueba TesoreriaApiTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VepApiTest {

    private static final String CUIT = "30556677881";
    // Ni el seed ni los tests crean una deuda con este CUIT.
    private static final String CUIT_WITHOUT_DEBT = "30000000000";
    private static final String PASSWORD = "clave-de-prueba";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DebtRepository debtRepository;

    @Autowired
    private TaxpayerRepository taxpayerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ObjectMapper objectMapper;

    private Debt debt;

    @BeforeEach
    void createDebtAndTaxpayer() {
        debt = debtRepository.saveAndFlush(new Debt(CUIT, new BigDecimal("1234.50")));
        taxpayerRepository.saveAndFlush(new Taxpayer(CUIT, "Contribuyente de prueba", passwordEncoder.encode(PASSWORD)));
    }

    @Test
    void debtIsPublic() throws Exception {
        mockMvc.perform(get("/api/vep/debts/{cuit}", CUIT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuit").value(CUIT))
                .andExpect(jsonPath("$.amount").value(1234.5))
                .andExpect(jsonPath("$.status").value("PENDING"))
                // El portal no expone el id interno de la deuda.
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    void forgivenDebtKeepsItsOriginalAmount() throws Exception {
        debt.forgive();
        // Sin flush y clear, la consulta devolveria la entidad que esta en memoria y no lo que se escribio
        // en la base.
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/vep/debts/{cuit}", CUIT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FORGIVEN"))
                .andExpect(jsonPath("$.amount").value(1234.5));
    }

    @Test
    void cuitWithoutDebtIsNotFound() throws Exception {
        mockMvc.perform(get("/api/vep/debts/{cuit}", CUIT_WITHOUT_DEBT))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void malformedCuitIsRejected() throws Exception {
        mockMvc.perform(get("/api/vep/debts/{cuit}", "20-12345678-9"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation"));
    }

    @Test
    void loginWithTheRightPasswordReturnsTheTaxpayer() throws Exception {
        mockMvc.perform(post("/api/vep/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(CUIT, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuit").value(CUIT))
                .andExpect(jsonPath("$.name").value("Contribuyente de prueba"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void loginWithAWrongPasswordIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/vep/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(CUIT, "otra-clave")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"))
                .andExpect(jsonPath("$.message").value("CUIT o clave incorrectos"));
    }

    @Test
    void loginWithACuitWithoutPasswordGivesTheSameError() throws Exception {
        mockMvc.perform(post("/api/vep/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(CUIT_WITHOUT_DEBT, PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("CUIT o clave incorrectos"));
    }

    @Test
    void loginWithAMalformedCuitIsRejected() throws Exception {
        mockMvc.perform(post("/api/vep/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("20-12345678-9", PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation"));
    }

    private String loginBody(String cuit, String password) throws Exception {
        return objectMapper.writeValueAsString(new VepLoginRequest(cuit, password));
    }

}
