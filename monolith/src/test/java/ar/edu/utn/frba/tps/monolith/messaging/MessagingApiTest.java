package ar.edu.utn.frba.tps.monolith.messaging;

import ar.edu.utn.frba.tps.monolith.messaging.dto.AuditEventDto;
import ar.edu.utn.frba.tps.monolith.messaging.model.TechnicalAccount;
import ar.edu.utn.frba.tps.monolith.messaging.model.TechnicalRole;
import ar.edu.utn.frba.tps.monolith.messaging.repository.TechnicalAccountRepository;
import ar.edu.utn.frba.tps.monolith.messaging.service.AuditEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba la API del modulo contra Postgres y RabbitMQ reales, con el esquema de db/init/ aplicado.
 * Cada test corre en una transaccion que se descarta al terminar. El envio de mensajes se reemplaza
 * por un mock para revisar el mensaje sin depender de quien este consumiendo la cola.
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
    private AuditEventPublisher publisher;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void createAccount() {
        accountRepository.save(new TechnicalAccount(USERNAME, passwordEncoder.encode(PASSWORD), TechnicalRole.READ, "TEST-SERVICE"));
    }

    @Test
    void endpointsRequireTheSessionOfTheModule() throws Exception {
        mockMvc.perform(get("/api/messaging/accounts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
        mockMvc.perform(get("/api/messaging/pending").header(AUTHORIZATION, "Bearer unknown"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/messaging/auth/../accounts"))
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

    @Test
    void publishedEventIsQueuedAndStaysPendingInTheModule() throws Exception {
        AuditEventDto event = publisher.publish("DEBT_FORGIVEN", "20123456789", Instant.parse("2026-10-04T21:15:30.123456789Z"));
        ArgumentCaptor<Message> sent = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq("auditoria.events"), sent.capture());
        // Se lee de la base y no de la cache de la sesion de JPA.
        entityManager.clear();

        String token = login();
        String stored = mockMvc.perform(get("/api/messaging/pending/{id}", event.id()).header(AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(event.id().toString()))
                .andExpect(jsonPath("$.type").value("DEBT_FORGIVEN"))
                .andExpect(jsonPath("$.cuit").value("20123456789"))
                .andExpect(jsonPath("$.timestamp").value("2026-10-04T21:15:30.123456Z"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        mockMvc.perform(get("/api/messaging/pending").header(AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(event.id().toString())));

        assertThat(sent.getValue().getMessageProperties().getContentType()).isEqualTo(MessageProperties.CONTENT_TYPE_JSON);
        assertThat(objectMapper.readTree(sent.getValue().getBody())).isEqualTo(objectMapper.readTree(stored));
    }

    @Test
    void eventWithAMalformedCuitIsNotPublished() {
        assertThatThrownBy(() -> publisher.publish("DEBT_FORGIVEN", "20-12345678-9", Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void unknownOrMalformedEventIdIsRejected() throws Exception {
        String token = login();

        mockMvc.perform(get("/api/messaging/pending/{id}", UUID.randomUUID()).header(AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
        mockMvc.perform(get("/api/messaging/pending/not-a-uuid").header(AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation"));
    }

    @Test
    void queueStatusReportsTheAuditQueue() throws Exception {
        String token = login();

        mockMvc.perform(get("/api/messaging/queue").header(AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queue").value("auditoria.events"))
                .andExpect(jsonPath("$.messages", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.consumers", greaterThanOrEqualTo(0)));
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
