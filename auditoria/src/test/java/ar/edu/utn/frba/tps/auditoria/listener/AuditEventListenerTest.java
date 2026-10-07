package ar.edu.utn.frba.tps.auditoria.listener;

import ar.edu.utn.frba.tps.auditoria.dto.AuditEventMessage;
import ar.edu.utn.frba.tps.auditoria.exception.MessagingUnavailableException;
import ar.edu.utn.frba.tps.auditoria.service.AuditEventProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AuditEventListenerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private AuditEventProcessor processor;

    @Test
    void parsesTheMessageAndDelegatesToTheProcessor() {
        AuditEventListener listener = new AuditEventListener(objectMapper, processor);
        UUID id = UUID.randomUUID();
        String json = "{\"id\":\"" + id + "\",\"type\":\"DEBT_FORGIVEN\",\"cuit\":\"20123456789\","
                + "\"timestamp\":\"2026-10-04T21:15:30.123456Z\",\"status\":\"PENDING\"}";

        listener.onMessage(message(json));

        ArgumentCaptor<AuditEventMessage> event = ArgumentCaptor.forClass(AuditEventMessage.class);
        verify(processor).process(event.capture());
        assertThat(event.getValue().id()).isEqualTo(id);
        assertThat(event.getValue().type()).isEqualTo("DEBT_FORGIVEN");
    }

    @Test
    void propagatesWhenProcessingFailsTransiently() {
        AuditEventListener listener = new AuditEventListener(objectMapper, processor);
        UUID id = UUID.randomUUID();
        String json = "{\"id\":\"" + id + "\",\"type\":\"DEBT_FORGIVEN\",\"cuit\":\"20123456789\","
                + "\"timestamp\":\"2026-10-04T21:15:30.123456Z\",\"status\":\"PENDING\"}";
        doThrow(new MessagingUnavailableException("modulo caido")).when(processor).process(any());

        assertThatThrownBy(() -> listener.onMessage(message(json)))
                .isInstanceOf(MessagingUnavailableException.class);
    }

    @Test
    void swallowsAMalformedMessageWithoutReachingTheProcessor() {
        AuditEventListener listener = new AuditEventListener(objectMapper, processor);

        listener.onMessage(message("esto no es json"));

        verifyNoInteractions(processor);
    }

    private Message message(String body) {
        return MessageBuilder.withBody(body.getBytes(StandardCharsets.UTF_8)).build();
    }

}
