package ar.edu.utn.frba.tps.auditoria.dto;

import java.util.List;

/**
 * Shape de la cookie de sesion del SGM (docs/contracts.md), tal como la decodifica
 * {@link ar.edu.utn.frba.tps.auditoria.service.SessionCookieCodec}. Es la misma que emite el monolito.
 */
public record Session(String username, String role, List<String> permissions) {
}
