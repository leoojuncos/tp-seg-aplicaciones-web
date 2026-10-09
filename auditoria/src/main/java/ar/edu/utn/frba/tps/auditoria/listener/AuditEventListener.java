package ar.edu.utn.frba.tps.auditoria.listener;

import ar.edu.utn.frba.tps.auditoria.dto.AuditEventMessage;
import ar.edu.utn.frba.tps.auditoria.service.AuditEventProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Escucha la cola de eventos de auditoria y delega el procesamiento. */
@Component
public class AuditEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);

    private final ObjectMapper objectMapper;
    private final AuditEventProcessor processor;

    public AuditEventListener(ObjectMapper objectMapper, AuditEventProcessor processor) {
        this.objectMapper = objectMapper;
        this.processor = processor;
    }

    @RabbitListener(queues = "${app.rabbitmq.queue}")
    public void onMessage(Message message) {
        AuditEventMessage event;
        try {
            event = objectMapper.readValue(message.getBody(), AuditEventMessage.class);
        } catch (IOException e) {
            // Un cuerpo que no es un evento valido no se recupera reintentando: se descarta.
            log.warn("Mensaje de auditoria mal formado ({} bytes): se descarta", message.getBody().length, e);
            return;
        }
        // Si el procesamiento falla por algo transitorio (el modulo de mensajeria caido), la
        // excepcion se propaga y el contenedor reencola el mensaje: se reintenta mas tarde en vez
        // de perder el registro. La demora previa a cada intento evita que el reintento sea un loop.
        processor.process(event);
    }

}
