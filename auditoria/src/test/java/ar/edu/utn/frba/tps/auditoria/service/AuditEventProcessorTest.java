package ar.edu.utn.frba.tps.auditoria.service;

import ar.edu.utn.frba.tps.auditoria.config.AuditoriaProperties;
import ar.edu.utn.frba.tps.auditoria.dto.AuditEventMessage;
import ar.edu.utn.frba.tps.auditoria.exception.MessagingUnavailableException;
import ar.edu.utn.frba.tps.auditoria.model.AuditEvent;
import ar.edu.utn.frba.tps.auditoria.repository.AuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditEventProcessorTest {

    private static final Instant NOW = Instant.parse("2026-10-06T12:00:00Z");
    private static final Instant OCCURRED = Instant.parse("2026-10-04T21:15:30.123456Z");

    @Mock
    private MessagingClient messagingClient;

    @Mock
    private AuditEventRepository repository;

    private AuditEventProcessor processor;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        processor = new AuditEventProcessor(messagingClient, repository, new AuditoriaProperties(Duration.ZERO), clock);
    }

    @Test
    void registersTheEventWithTheModulesDataNotTheMessages() {
        UUID id = UUID.randomUUID();
        // El mensaje de la cola trae datos distintos a los del modulo: simula un mensaje adulterado
        // con el id de un pendiente real. Lo que se registra tiene que salir del modulo.
        AuditEventMessage message = new AuditEventMessage(id, "DEBT_FORGIVEN", "20999999999",
                Instant.parse("2020-01-01T00:00:00Z"), "PENDING");
        AuditEventMessage confirmed = new AuditEventMessage(id, "DEBT_FORGIVEN", "20123456789", OCCURRED, "PENDING");
        when(repository.existsById(id)).thenReturn(false);
        when(messagingClient.findPending(id)).thenReturn(Optional.of(confirmed));

        processor.process(message);

        ArgumentCaptor<AuditEvent> saved = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getId()).isEqualTo(id);
        assertThat(saved.getValue().getCuit()).isEqualTo("20123456789");
        assertThat(saved.getValue().getOccurredAt()).isEqualTo(OCCURRED);
        assertThat(saved.getValue().getRegisteredAt()).isEqualTo(NOW);
    }

    @Test
    void retriesWhenTheModuleReportsAnUnknownStatus() {
        AuditEventMessage event = event("INESPERADO");
        when(repository.existsById(event.id())).thenReturn(false);
        when(messagingClient.findPending(event.id())).thenReturn(Optional.of(event("INESPERADO", event.id())));

        assertThatThrownBy(() -> processor.process(event)).isInstanceOf(MessagingUnavailableException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void discardsTheEventWhenItWasCancelled() {
        AuditEventMessage event = event("PENDING");
        when(repository.existsById(event.id())).thenReturn(false);
        when(messagingClient.findPending(event.id())).thenReturn(Optional.of(event("CANCELLED", event.id())));

        processor.process(event);

        verify(repository, never()).save(any());
    }

    @Test
    void discardsTheEventWhenItIsNoLongerPending() {
        AuditEventMessage event = event("PENDING");
        when(repository.existsById(event.id())).thenReturn(false);
        when(messagingClient.findPending(event.id())).thenReturn(Optional.empty());

        processor.process(event);

        verify(repository, never()).save(any());
    }

    @Test
    void propagatesWhenTheModuleIsUnavailable() {
        AuditEventMessage event = event("PENDING");
        when(repository.existsById(event.id())).thenReturn(false);
        when(messagingClient.findPending(event.id())).thenThrow(new MessagingUnavailableException("modulo caido"));

        assertThatThrownBy(() -> processor.process(event)).isInstanceOf(MessagingUnavailableException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void skipsTheEventWhenItWasAlreadyRegistered() {
        AuditEventMessage event = event("PENDING");
        when(repository.existsById(event.id())).thenReturn(true);

        processor.process(event);

        verify(repository, never()).save(any());
        verifyNoInteractions(messagingClient);
    }

    private AuditEventMessage event(String status) {
        return event(status, UUID.randomUUID());
    }

    private AuditEventMessage event(String status, UUID id) {
        return new AuditEventMessage(id, "DEBT_FORGIVEN", "20123456789", OCCURRED, status);
    }

}
