package ar.edu.utn.frba.tps.monolith.messaging.service;

import ar.edu.utn.frba.tps.monolith.messaging.config.MessagingProperties;
import ar.edu.utn.frba.tps.monolith.messaging.dto.QueueStatusResponse;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QueueStatusService {

    private final AmqpAdmin amqpAdmin;
    private final String queue;

    @Autowired
    public QueueStatusService(AmqpAdmin amqpAdmin, MessagingProperties properties) {
        this.amqpAdmin = amqpAdmin;
        this.queue = properties.queue();
    }

    public QueueStatusResponse status() {
        QueueInformation info = amqpAdmin.getQueueInfo(queue);
        if (info == null) {
            return new QueueStatusResponse(queue, 0, 0);
        }
        return new QueueStatusResponse(queue, info.getMessageCount(), info.getConsumerCount());
    }

}
