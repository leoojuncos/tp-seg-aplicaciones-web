package ar.edu.utn.frba.tps.monolith.config;

import ar.edu.utn.frba.tps.monolith.auth.service.SessionCookieCodec;
import ar.edu.utn.frba.tps.monolith.filter.SessionCookieFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class AuthConfig {

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
