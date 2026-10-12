package ar.edu.utn.frba.tps.auditoria.service;

import ar.edu.utn.frba.tps.auditoria.dto.Session;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * Lee la cookie de sesion del SGM: base64 (url-safe, sin padding) de un JSON {@link Session}, sin
 * firma. Auditoria solo la decodifica (no emite sesiones). Que la cookie no este firmada ni se
 * re-derive server-side es la vulnerabilidad #2 del escenario (ver AGENTS.md): no se corrige aca.
 */
@Component
public class SessionCookieCodec {

    private final ObjectMapper objectMapper;

    public SessionCookieCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<Session> decode(String cookieValue) {
        try {
            byte[] json = Base64.getUrlDecoder().decode(cookieValue);
            // ofNullable: el JSON "null" es base64 valido y Jackson lo decodifica a null, no a una
            // excepcion; con Optional.of eso seria una NPE fuera del catch.
            return Optional.ofNullable(
                    objectMapper.readValue(new String(json, StandardCharsets.UTF_8), Session.class));
        } catch (IllegalArgumentException | JsonProcessingException e) {
            return Optional.empty();
        }
    }

}
