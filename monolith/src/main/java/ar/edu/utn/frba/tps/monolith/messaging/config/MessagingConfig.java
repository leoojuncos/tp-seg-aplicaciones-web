package ar.edu.utn.frba.tps.monolith.messaging.config;

import ar.edu.utn.frba.tps.monolith.messaging.filter.TechnicalSessionFilter;
import ar.edu.utn.frba.tps.monolith.messaging.service.TechnicalSessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableConfigurationProperties(MessagingProperties.class)
public class MessagingConfig {

    /**
     * Declara la cola de eventos de auditoria: el RabbitAdmin de Spring Boot la crea en el broker al
     * conectarse. Auditoria la declara con las mismas propiedades (durable, no exclusiva, sin
     * autodelete); si difirieran, RabbitMQ rechazaria la segunda declaracion.
     */
    @Bean
    public Queue auditEventsQueue(MessagingProperties properties) {
        return new Queue(properties.queue(), true);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Titula la documentacion OpenAPI como la del servicio de mensajeria. Junto con
     * {@code springdoc.paths-to-match} (acotado a /api/messaging/**, ver application.yml), hace que Swagger
     * se vea como la API de ese servicio y no la del resto del monolito. Queda servido sin sesion a
     * proposito (vulnerabilidad #3, ver AGENTS.md).
     */
    @Bean
    public OpenAPI messagingOpenApi() {
        return new OpenAPI().info(new Info()
                .title("SGM - Servicio de Mensajeria")
                .description("API interna del servicio de mensajeria del SGM: autenticacion de cuentas "
                        + "tecnicas, cuentas, estado de la cola y eventos pendientes.")
                .version("v1"));
    }

    /**
     * Registra el filtro de sesion solo para las rutas del modulo. El filtro no es un
     * {@code @Component}: si lo fuera, Spring Boot lo registraria tambien para todas las URLs.
     */
    @Bean
    public FilterRegistrationBean<TechnicalSessionFilter> technicalSessionFilter(
            TechnicalSessionService sessionService, ObjectMapper objectMapper) {
        FilterRegistrationBean<TechnicalSessionFilter> registration =
                new FilterRegistrationBean<>(new TechnicalSessionFilter(sessionService, objectMapper));
        registration.addUrlPatterns("/api/messaging/*");
        return registration;
    }

}
