package ar.edu.utn.frba.tps.monolith.auth.service;

import ar.edu.utn.frba.tps.monolith.auth.dto.SessionResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * Codec de la cookie de sesion del SGM: base64 (url-safe, sin padding) de un JSON
 * {@link SessionResponse}, sin firma. No valida el rol ni los permisos contra la base: esta
 * sesion no se re-deriva server-side, es la vulnerabilidad #2 del escenario (ver AGENTS.md).
 */
@Component
public class SessionCookieCodec {

    private final ObjectMapper objectMapper;

    public SessionCookieCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String encode(SessionResponse session) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(session);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("No se pudo serializar la sesion", e);
        }
    }

    public Optional<SessionResponse> decode(String cookieValue) {
        try {
            byte[] json = Base64.getUrlDecoder().decode(cookieValue);
            // ofNullable, no of: el JSON "null" es base64 valido y Jackson lo decodifica a null,
            // no a una excepcion. Con Optional.of eso era una NPE que escapaba del catch (F1).
            return Optional.ofNullable(objectMapper.readValue(new String(json, StandardCharsets.UTF_8), SessionResponse.class));
        } catch (IllegalArgumentException | JsonProcessingException e) {
            return Optional.empty();
        }
    }

}
