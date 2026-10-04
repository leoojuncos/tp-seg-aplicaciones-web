package ar.edu.utn.frba.tps.monolith.messaging;

import ar.edu.utn.frba.tps.monolith.messaging.model.TechnicalAccount;
import ar.edu.utn.frba.tps.monolith.messaging.model.TechnicalRole;
import ar.edu.utn.frba.tps.monolith.messaging.repository.TechnicalAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba la API del modulo contra Postgres y RabbitMQ reales, con el esquema de db/init/ aplicado.
 * Cada test corre en una transaccion que se descarta al terminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MessagingApiTest {

    private static final String USERNAME = "test_reader";
    private static final String PASSWORD = "test-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TechnicalAccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void createAccount() {
        accountRepository.save(new TechnicalAccount(USERNAME, passwordEncoder.encode(PASSWORD), TechnicalRole.READ, "TEST-SERVICE"));
    }

    @Test
    void endpointsRequireTheSessionOfTheModule() throws Exception {
        mockMvc.perform(get("/api/messaging/accounts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
        mockMvc.perform(get("/api/messaging/accounts").header(AUTHORIZATION, "Bearer unknown"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithAWrongPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/api/messaging/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(USERNAME, "wrong")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    void loginRequiresUsernameAndPassword() throws Exception {
        mockMvc.perform(post("/api/messaging/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation"));
    }

    @Test
    void sessionListsTheTechnicalAccountsWithoutTheirPasswords() throws Exception {
        String token = login();

        mockMvc.perform(get("/api/messaging/accounts").header(AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username == '" + USERNAME + "')].role", contains("READ")))
                .andExpect(jsonPath("$[?(@.username == '" + USERNAME + "')].serviceId", contains("TEST-SERVICE")))
                .andExpect(content().string(not(containsString("password"))));
    }

    private String login() throws Exception {
        String response = mockMvc.perform(post("/api/messaging/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(USERNAME, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(USERNAME))
                .andExpect(jsonPath("$.role").value("READ"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private String loginBody(String username, String password) throws Exception {
        return objectMapper.writeValueAsString(new LoginBody(username, password));
    }

    private record LoginBody(String username, String password) {
    }

}
