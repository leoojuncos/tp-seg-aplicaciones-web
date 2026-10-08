package ar.edu.utn.frba.tps.monolith.auth.controller;

import ar.edu.utn.frba.tps.monolith.auth.dto.SessionResponse;
import ar.edu.utn.frba.tps.monolith.config.AuthProperties;
import ar.edu.utn.frba.tps.monolith.filter.SessionCookieFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de la sesion del SGM. El filtro {@link SessionCookieFilter} ya exige una cookie
 * valida para llegar hasta aca: ninguno de los dos metodos necesita manejar el caso sin sesion.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthProperties properties;

    public AuthController(AuthProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/session")
    public SessionResponse session(HttpServletRequest request) {
        return SessionCookieFilter.currentSession(request)
                .orElseThrow(() -> new IllegalStateException("El filtro no dejo la sesion en el request"));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie expired = ResponseCookie.from(properties.cookieName(), "")
                .path("/")
                .httpOnly(true)
                .sameSite("Lax")
                .maxAge(0)
                .build();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expired.toString())
                .build();
    }

}
