package ar.edu.utn.frba.tps.auditoria.filter;

import ar.edu.utn.frba.tps.auditoria.config.AuthProperties;
import ar.edu.utn.frba.tps.auditoria.dto.ErrorResponse;
import ar.edu.utn.frba.tps.auditoria.dto.Session;
import ar.edu.utn.frba.tps.auditoria.service.SessionCookieCodec;
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
import java.util.List;
import java.util.Optional;

/**
 * Gating de Auditoria: exige la cookie de sesion del SGM con el permiso {@code AUDITORIA} en todo
 * {@code /api/**}, salvo {@code /api/health}. Usa la misma cookie que el monolito; como Auditoria es
 * un solo modulo, resuelve la sesion y su permiso en un filtro. Responde con el formato de error del
 * SGM (docs/contracts.md).
 *
 * <p>No es un {@code @Component}: se registra en AuditoriaConfig para fijar sus rutas.
 */
public class SessionCookieFilter extends OncePerRequestFilter {

    private static final String PERMISSION = "AUDITORIA";
    private static final String HEALTH_PATH = "/api/health";

    private final AuthProperties properties;
    private final SessionCookieCodec codec;
    private final ObjectMapper objectMapper;

    public SessionCookieFilter(AuthProperties properties, SessionCookieCodec codec, ObjectMapper objectMapper) {
        this.properties = properties;
        this.codec = codec;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return HEALTH_PATH.equals(normalized(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Optional<Session> session = cookieValue(request).flatMap(codec::decode);
        if (session.isEmpty()) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "unauthorized", "No hay sesión, o no es válida");
            return;
        }
        List<String> permissions = session.get().permissions();
        if (permissions == null || !permissions.contains(PERMISSION)) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "forbidden",
                    "La sesión no tiene permiso para este módulo");
            return;
        }
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

    // getServletPath()/getPathInfo() ya vienen decodificados; cleanPath resuelve segmentos como ".."
    // para que una ruta no entre a /api/** haciendose pasar por /api/health.
    private static String normalized(HttpServletRequest request) {
        String rawPath = request.getServletPath() + (request.getPathInfo() != null ? request.getPathInfo() : "");
        return StringUtils.cleanPath(rawPath);
    }

    private void writeError(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(code, message));
    }

}
