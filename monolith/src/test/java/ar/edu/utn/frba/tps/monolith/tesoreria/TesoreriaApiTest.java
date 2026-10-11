package ar.edu.utn.frba.tps.monolith.tesoreria;

import ar.edu.utn.frba.tps.monolith.auth.dto.SessionResponse;
import ar.edu.utn.frba.tps.monolith.auth.service.SessionCookieCodec;
import ar.edu.utn.frba.tps.monolith.messaging.dto.AuditEventDto;
import ar.edu.utn.frba.tps.monolith.messaging.model.PendingEventStatus;
import ar.edu.utn.frba.tps.monolith.messaging.repository.PendingEventRepository;
import ar.edu.utn.frba.tps.monolith.messaging.service.AuditEventPublisher;
import ar.edu.utn.frba.tps.monolith.messaging.service.PendingEventService;
import ar.edu.utn.frba.tps.monolith.tesoreria.model.Debt;
import ar.edu.utn.frba.tps.monolith.tesoreria.model.DebtStatus;
import ar.edu.utn.frba.tps.monolith.tesoreria.repository.DebtRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba la API de deudas contra Postgres, con el esquema de db/init/ aplicado. Cada test crea su
 * deuda, con un CUIT que el seed no usa, en una transaccion que se descarta al terminar; el del rollback
 * de la condonacion corre sin ella y borra su deuda al final. La cookie de sesion se arma con el
 * SessionCookieCodec real, como en AuthApiTest. El envio a RabbitMQ se reemplaza por un mock, como en
 * MessagingApiTest, para revisar el evento que entrega la condonacion sin depender de quien consuma la
 * cola.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TesoreriaApiTest {

    private static final String CUIT = "33698765432";
    private static final String COOKIE_NAME = "sgm_session";
    private static final String QUEUE = "auditoria.events";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DebtRepository debtRepository;

    @Autowired
    private PendingEventRepository pendingEventRepository;

    @Autowired
    private PendingEventService pendingEventService;

    @Autowired
    private SessionCookieCodec codec;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

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

    @Test
    void forgivingQueuesTheAuditEvent() throws Exception {
        Instant before = Instant.now();
        mockMvc.perform(post("/api/tesoreria/debts/{id}/forgive", debt.getId()).cookie(session))
                .andExpect(status().isOk());
        Instant after = Instant.now();

        JsonNode message = sentMessage();
        // Se lee de la base y no de la cache de la sesion de JPA.
        entityManager.clear();
        AuditEventDto stored = pendingEventService.get(UUID.fromString(message.get("id").asText()));
        assertThat(stored.type()).isEqualTo(AuditEventPublisher.DEBT_FORGIVEN);
        assertThat(stored.cuit()).isEqualTo(CUIT);
        assertThat(stored.status()).isEqualTo(PendingEventStatus.PENDING);
        // El modulo trunca el timestamp a microsegundos.
        assertThat(stored.timestamp()).isBetween(before.truncatedTo(ChronoUnit.MICROS), after);
        assertThat(message).isEqualTo(objectMapper.readTree(objectMapper.writeValueAsBytes(stored)));
    }

    // Sin la transaccion del test: con ella, la condonacion se uniria a esa transaccion y la excepcion
    // solo la marcaria rollback-only, con el UPDATE de la deuda ya escrito. Asi la condonacion abre la
    // suya y se ve el rollback real. La deuda del BeforeEach queda guardada en la base: se borra al final.
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void forgivingIsRolledBackWhenTheEventCannotBeQueued() throws Exception {
        try {
            doThrow(new AmqpConnectException(new ConnectException("Connection refused")))
                    .when(rabbitTemplate).send(eq(QUEUE), any(Message.class));

            mockMvc.perform(post("/api/tesoreria/debts/{id}/forgive", debt.getId()).cookie(session))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.error").value("rabbitmq_unavailable"));

            UUID eventId = UUID.fromString(sentMessage().get("id").asText());
            assertThat(pendingEventRepository.existsById(eventId)).isFalse();
            assertThat(debtRepository.findById(debt.getId())).get()
                    .extracting(Debt::getStatus).isEqualTo(DebtStatus.PENDING);
        } finally {
            debtRepository.deleteById(debt.getId());
        }
    }

    // El mensaje que el modulo messaging le entrego a la cola de Auditoria.
    private JsonNode sentMessage() throws IOException {
        ArgumentCaptor<Message> sent = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq(QUEUE), sent.capture());
        return objectMapper.readTree(sent.getValue().getBody());
    }

}
