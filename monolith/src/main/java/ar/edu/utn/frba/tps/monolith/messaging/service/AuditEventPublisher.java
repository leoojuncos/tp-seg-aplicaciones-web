package ar.edu.utn.frba.tps.monolith.messaging.service;

import ar.edu.utn.frba.tps.monolith.messaging.config.MessagingProperties;
import ar.edu.utn.frba.tps.monolith.messaging.dto.AuditEventDto;
import ar.edu.utn.frba.tps.monolith.messaging.mapper.MessagingMapper;
import ar.edu.utn.frba.tps.monolith.messaging.model.PendingEvent;
import ar.edu.utn.frba.tps.monolith.messaging.repository.PendingEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.regex.Pattern;

/**
 * Punto de entrada de los demas modulos del monolito para publicar un evento de auditoria. Ningun
 * otro modulo accede a la cola: el evento se entrega aca, que lo guarda como pendiente y lo encola.
 */
@Service
public class AuditEventPublisher {

    private static final Pattern CUIT = Pattern.compile("\\d{11}");

    private final PendingEventRepository pendingEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final MessagingMapper mapper;
    private final String queue;

    @Autowired
    public AuditEventPublisher(PendingEventRepository pendingEventRepository, RabbitTemplate rabbitTemplate,
                               ObjectMapper objectMapper, MessagingMapper mapper, MessagingProperties properties) {
        this.pendingEventRepository = pendingEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.mapper = mapper;
        this.queue = properties.queue();
    }

    /**
     * Guarda el evento como pendiente y lo encola. Si RabbitMQ no acepta el mensaje, la excepcion
     * revierte la transaccion y el evento no queda guardado.
     */
    @Transactional
    public AuditEventDto publish(String type, String cuit, Instant timestamp) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("El tipo de evento es obligatorio");
        }
        if (cuit == null || !CUIT.matcher(cuit).matches()) {
            throw new IllegalArgumentException("El CUIT tiene que tener 11 digitos, sin guiones");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("El timestamp del evento es obligatorio");
        }

        PendingEvent event = pendingEventRepository.saveAndFlush(new PendingEvent(type, cuit, timestamp));
        AuditEventDto dto = mapper.toDto(event);
        rabbitTemplate.send(queue, toMessage(dto));
        return dto;
    }

    private Message toMessage(AuditEventDto dto) {
        try {
            return MessageBuilder.withBody(objectMapper.writeValueAsBytes(dto))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setContentEncoding(StandardCharsets.UTF_8.name())
                    .build();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el evento " + dto.id(), e);
        }
    }

}
