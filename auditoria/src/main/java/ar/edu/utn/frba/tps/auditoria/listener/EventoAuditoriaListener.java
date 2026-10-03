package ar.edu.utn.frba.tps.auditoria.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listener placeholder: deja al micro conectado y escuchando la cola, pero todavia no
 * procesa el evento. Recibe el mensaje crudo (sin deserializar a un tipo) para no atar
 * el esqueleto a un contrato que todavia no esta cerrado.
 *
 * <p>El consumo real -demora deliberada, evento pendiente como ventana del paso 4 y
 * persistencia del {@code EventoAuditoria}- se implementa en TPS-22.
 */
@Component
public class EventoAuditoriaListener {

    private static final Logger log = LoggerFactory.getLogger(EventoAuditoriaListener.class);

    @RabbitListener(queues = "${app.rabbitmq.queue}")
    public void recibir(Message message) {
        log.info("Evento de auditoria recibido ({} bytes). Placeholder: sin procesar todavia (TPS-22).",
                message.getBody().length);
    }

}
