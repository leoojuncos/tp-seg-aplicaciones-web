package ar.edu.utn.frba.tps.monolith.filter;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * Rutas de {@code /api/**} tal como las comparan los filtros del SGM: la ruta normalizada, cuales son
 * publicas (no piden la cookie de sesion) y el modulo al que pertenece una ruta.
 */
final class ApiPaths {

    // Prefijos porque liberan subarboles enteros (/api/vep/**, /api/messaging/**), a diferencia de
    // TechnicalSessionFilter, que compara paths exactos.
    private static final Set<String> PUBLIC_EXACT_PATHS = Set.of("/api/health", "/api/auth/login");
    private static final Set<String> PUBLIC_PATH_PREFIXES = Set.of("/api/vep/", "/api/messaging/");

    private static final String API_PREFIX = "/api/";

    private ApiPaths() {
    }

    /**
     * La ruta del request, normalizada. getServletPath()/getPathInfo() ya vienen decodificados y
     * normalizados por el contenedor (a diferencia de getRequestURI(), que es la URI cruda), y
     * cleanPath resuelve lo que quede: sin esto, "/api/messaging/../auth/session" empieza
     * literalmente con "/api/messaging/" y quedaria sin proteccion, y trucos como "..;" o "%2e%2e"
     * en el path pasarian igual.
     */
    static String normalized(HttpServletRequest request) {
        String rawPath = request.getServletPath() + (request.getPathInfo() != null ? request.getPathInfo() : "");
        return StringUtils.cleanPath(rawPath);
    }

    static boolean isPublic(String path) {
        return PUBLIC_EXACT_PATHS.contains(path) || PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    /** El primer segmento despues de {@code /api/} ({@code tesoreria} en /api/tesoreria/debts), o "" si no hay. */
    static String module(String path) {
        if (!path.startsWith(API_PREFIX)) {
            return "";
        }
        String rest = path.substring(API_PREFIX.length());
        int slash = rest.indexOf('/');
        return slash < 0 ? rest : rest.substring(0, slash);
    }

}
