package ar.edu.utn.frba.tps.monolith.config;

import ar.edu.utn.frba.tps.monolith.auth.service.SessionCookieCodec;
import ar.edu.utn.frba.tps.monolith.filter.ModulePermissionFilter;
import ar.edu.utn.frba.tps.monolith.filter.SessionCookieFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class AuthConfig {

    // Primero se resuelve la sesion y despues se mira su permiso: el gating lee la sesion que dejo
    // el filtro de la cookie.
    private static final int SESSION_COOKIE_FILTER_ORDER = 1;
    private static final int MODULE_PERMISSION_FILTER_ORDER = 2;

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
        registration.setOrder(SESSION_COOKIE_FILTER_ORDER);
        return registration;
    }

    /** Registra el gating de modulos para todo {@code /api/**}, despues del filtro de la cookie. */
    @Bean
    public FilterRegistrationBean<ModulePermissionFilter> modulePermissionFilter(ObjectMapper objectMapper) {
        FilterRegistrationBean<ModulePermissionFilter> registration =
                new FilterRegistrationBean<>(new ModulePermissionFilter(objectMapper));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(MODULE_PERMISSION_FILTER_ORDER);
        return registration;
    }

}
