package ar.edu.utn.frba.tps.monolith.auth.dto;

import java.util.List;

/**
 * Shape de la cookie de sesion del SGM y de la respuesta de {@code GET /api/auth/session}.
 * Viaja tal cual la decodifica {@link ar.edu.utn.frba.tps.monolith.auth.service.SessionCookieCodec}.
 */
public record SessionResponse(String username, String role, List<String> permissions) {
}
