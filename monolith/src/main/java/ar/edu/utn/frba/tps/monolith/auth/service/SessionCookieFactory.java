package ar.edu.utn.frba.tps.monolith.auth.service;

import ar.edu.utn.frba.tps.monolith.auth.dto.SessionResponse;
import ar.edu.utn.frba.tps.monolith.config.AuthProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** Centraliza los atributos de la cookie de sesion del SGM (Path=/; HttpOnly; SameSite=Lax; sin Secure). */
@Component
public class SessionCookieFactory {

    private final AuthProperties properties;
    private final SessionCookieCodec codec;

    public SessionCookieFactory(AuthProperties properties, SessionCookieCodec codec) {
        this.properties = properties;
        this.codec = codec;
    }

    public ResponseCookie issue(SessionResponse session) {
        return builder(codec.encode(session)).build();
    }

    public ResponseCookie expire() {
        return builder("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder builder(String value) {
        return ResponseCookie.from(properties.cookieName(), value)
                .path("/")
                .httpOnly(true)
                .sameSite("Lax");
    }

}
