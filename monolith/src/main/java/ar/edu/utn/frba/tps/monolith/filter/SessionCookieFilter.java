package ar.edu.utn.frba.tps.monolith.filter;

import ar.edu.utn.frba.tps.monolith.auth.dto.SessionResponse;
import ar.edu.utn.frba.tps.monolith.auth.service.SessionCookieCodec;
import ar.edu.utn.frba.tps.monolith.config.AuthProperties;
import ar.edu.utn.frba.tps.monolith.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

/**
 * Exige la cookie de sesion del SGM en todo {@code /api/**}, salvo las rutas publicas. Deja la
 * sesion resuelta en el atributo {@link #SESSION_ATTRIBUTE} del request, para que el resto de los
 * modulos tome los permisos de ahi sin re-derivarlos (esa falta de re-derivacion es la
 * vulnerabilidad #2 del escenario, ver AGENTS.md: no se corrige en este filtro).
 *
 * <p>No es un {@code @Component}: si lo fuera, Spring Boot lo registraria tambien para todas las
 * URLs (igual motivo que {@code TechnicalSessionFilter} de messaging).
 */
public class SessionCookieFilter extends OncePerRequestFilter {

    public static final String SESSION_ATTRIBUTE = "sgmSession";

    // A diferencia de TechnicalSessionFilter (paths exactos), esta lista usa prefijos porque
    // tiene que liberar subarboles enteros (/api/vep/**, /api/messaging/**). Por eso la URI se
    // normaliza con StringUtils.cleanPath antes de comparar: sin esto, "/api/messaging/../auth/
    // session" empieza literalmente con "/api/messaging/" y quedaria sin protección.
    private static final Set<String> PUBLIC_EXACT_PATHS = Set.of("/api/health", "/api/auth/login");
    private static final Set<String> PUBLIC_PATH_PREFIXES = Set.of("/api/vep/", "/api/messaging/");

    private final AuthProperties properties;
    private final SessionCookieCodec codec;
    private final ObjectMapper objectMapper;

    public SessionCookieFilter(AuthProperties properties, SessionCookieCodec codec, ObjectMapper objectMapper) {
        this.properties = properties;
        this.codec = codec;
        this.objectMapper = objectMapper;
    }

    public static Optional<SessionResponse> currentSession(HttpServletRequest request) {
        return Optional.ofNullable((SessionResponse) request.getAttribute(SESSION_ATTRIBUTE));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // getServletPath()/getPathInfo() ya vienen decodificados y normalizados por el contenedor
        // (a diferencia de getRequestURI(), que es la URI cruda): cleanPath solo no alcanzaba para
        // trucos como "..;" o "%2e%2e" en el path (I1).
        String rawPath = request.getServletPath() + (request.getPathInfo() != null ? request.getPathInfo() : "");
        String path = StringUtils.cleanPath(rawPath);
        if (PUBLIC_EXACT_PATHS.contains(path)) {
            return true;
        }
        return PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Optional<SessionResponse> session = cookieValue(request).flatMap(codec::decode);
        if (session.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getOutputStream(),
                    new ErrorResponse("unauthorized", "No hay sesión, o no es válida"));
            return;
        }
        request.setAttribute(SESSION_ATTRIBUTE, session.get());
        chain.doFilter(request, response);
    }

    private Optional<String> cookieValue(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (properties.cookieName().equals(cookie.getName())) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }

}
