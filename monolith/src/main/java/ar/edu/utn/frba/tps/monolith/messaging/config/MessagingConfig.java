package ar.edu.utn.frba.tps.monolith.messaging.config;

import ar.edu.utn.frba.tps.monolith.messaging.filter.TechnicalSessionFilter;
import ar.edu.utn.frba.tps.monolith.messaging.service.TechnicalSessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableConfigurationProperties(MessagingProperties.class)
public class MessagingConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
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
