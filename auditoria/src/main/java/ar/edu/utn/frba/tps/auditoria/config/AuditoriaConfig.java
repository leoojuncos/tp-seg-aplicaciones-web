package ar.edu.utn.frba.tps.auditoria.config;

import ar.edu.utn.frba.tps.auditoria.filter.SessionCookieFilter;
import ar.edu.utn.frba.tps.auditoria.service.SessionCookieCodec;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({MessagingProperties.class, AuditoriaProperties.class, AuthProperties.class})
public class AuditoriaConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * Registra el filtro de la cookie de sesion para todo {@code /api/**}. El filtro no es un
     * {@code @Component}: si lo fuera, Spring Boot lo registraria con su propio criterio.
     */
    @Bean
    public FilterRegistrationBean<SessionCookieFilter> sessionCookieFilter(
            AuthProperties properties, SessionCookieCodec codec, ObjectMapper objectMapper) {
        FilterRegistrationBean<SessionCookieFilter> registration =
                new FilterRegistrationBean<>(new SessionCookieFilter(properties, codec, objectMapper));
        registration.addUrlPatterns("/api/*");
        return registration;
    }

}
