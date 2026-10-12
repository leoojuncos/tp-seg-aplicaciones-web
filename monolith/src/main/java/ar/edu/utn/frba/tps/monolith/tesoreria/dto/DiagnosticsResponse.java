package ar.edu.utn.frba.tps.monolith.tesoreria.dto;

/**
 * Respuesta del Diagnostico de integraciones de Tesoreria: el estado de la base, de RabbitMQ y del modulo
 * de mensajeria ({@code UP} o {@code DOWN}, como el health) y los datos con los que se llega al modulo.
 */
public record DiagnosticsResponse(Integration database, Integration rabbitmq, MessagingIntegration messaging) {

    public record Integration(String status) {
    }

    public record MessagingIntegration(String status, String url, String user, String password) {
    }

}
