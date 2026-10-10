package ar.edu.utn.frba.tps.monolith.filter;

import ar.edu.utn.frba.tps.monolith.auth.dto.SessionResponse;
import ar.edu.utn.frba.tps.monolith.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

/**
 * Gating de modulos (TPS-15): {@code /api/<modulo>/**} exige el permiso con el nombre del modulo en
 * mayuscula ({@code /api/tesoreria/**} pide {@code TESORERIA}). Corre despues de
 * {@link SessionCookieFilter}, que ya dejo la sesion en el request (o respondio 401), y saltea las
 * mismas rutas publicas.
 *
 * <p>Los permisos salen de la cookie tal cual, sin volver a derivarlos de la base a partir del
 * usuario: es la vulnerabilidad #2 del escenario (ver AGENTS.md) y no se corrige en este filtro.
 *
 * <p>Default deny: lo que no es publico, ni de {@code auth} (que pide solo sesion), ni de un modulo
 * de {@link #MODULES} responde 403. Un modulo nuevo del monolito se suma ahi.
 */
public class ModulePermissionFilter extends OncePerRequestFilter {

    // Modulos del monolito con rutas bajo /api. VEP es publico y messaging tiene su propia sesion
    // (ver ApiPaths); Auditoria es otro servicio, con su propio filtro.
    private static final Set<String> MODULES = Set.of("administracion", "tesoreria");
    // Rutas de la propia sesion (GET /api/auth/session, POST /api/auth/logout): alcanza con tenerla.
    private static final String SESSION_ONLY_MODULE = "auth";

    private final ObjectMapper objectMapper;

    public ModulePermissionFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return ApiPaths.isPublic(ApiPaths.normalized(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String module = ApiPaths.module(ApiPaths.normalized(request));
        if (module.equals(SESSION_ONLY_MODULE) || (MODULES.contains(module) && hasPermission(request, module))) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(),
                new ErrorResponse("forbidden", "La sesión no tiene permiso para este módulo"));
    }

    private static boolean hasPermission(HttpServletRequest request, String module) {
        String permission = module.toUpperCase(Locale.ROOT);
        return SessionCookieFilter.currentSession(request)
                .map(SessionResponse::permissions)
                .map(permissions -> permissions != null && permissions.contains(permission))
                .orElse(false);
    }

}
