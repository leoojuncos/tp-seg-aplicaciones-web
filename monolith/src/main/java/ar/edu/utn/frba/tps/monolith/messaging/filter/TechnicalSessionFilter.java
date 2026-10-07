package ar.edu.utn.frba.tps.monolith.messaging.filter;

import ar.edu.utn.frba.tps.monolith.dto.ErrorResponse;
import ar.edu.utn.frba.tps.monolith.messaging.model.TechnicalSession;
import ar.edu.utn.frba.tps.monolith.messaging.service.TechnicalSessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

/**
 * Exige la sesion propia del modulo ({@code Authorization: Bearer <token>}) en todas sus rutas
 * salvo las que emiten la sesion. No usa la cookie de sesion del SGM. Deja la sesion resuelta en
 * el atributo {@link #SESSION_ATTRIBUTE} del request.
 */
public class TechnicalSessionFilter extends OncePerRequestFilter {

    public static final String SESSION_ATTRIBUTE = "messagingSession";

    // Se compara la ruta exacta y no un prefijo, para que una ruta armada con segmentos como ".."
    // no pase como publica.
    private static final Set<String> PUBLIC_PATHS = Set.of("/api/messaging/auth/login");
    private static final String BEARER_PREFIX = "Bearer ";

    private final TechnicalSessionService sessionService;
    private final ObjectMapper objectMapper;

    public TechnicalSessionFilter(TechnicalSessionService sessionService, ObjectMapper objectMapper) {
        this.sessionService = sessionService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return PUBLIC_PATHS.contains(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Optional<TechnicalSession> session = bearerToken(request).flatMap(sessionService::resolve);
        if (session.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getOutputStream(),
                    new ErrorResponse("unauthorized", "Token ausente, desconocido o vencido"));
            return;
        }
        request.setAttribute(SESSION_ATTRIBUTE, session.get());
        chain.doFilter(request, response);
    }

    private static Optional<String> bearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        String token = header.substring(BEARER_PREFIX.length()).strip();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }

}
