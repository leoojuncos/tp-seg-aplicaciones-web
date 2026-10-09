package ar.edu.utn.frba.tps.monolith.auth.controller;

import ar.edu.utn.frba.tps.monolith.auth.dto.SessionResponse;
import ar.edu.utn.frba.tps.monolith.auth.service.SessionCookieFactory;
import ar.edu.utn.frba.tps.monolith.filter.SessionCookieFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
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

    private final SessionCookieFactory cookieFactory;

    public AuthController(SessionCookieFactory cookieFactory) {
        this.cookieFactory = cookieFactory;
    }

    @GetMapping("/session")
    public SessionResponse session(HttpServletRequest request) {
        return SessionCookieFilter.currentSession(request)
                .orElseThrow(() -> new IllegalStateException("El filtro no dejo la sesion en el request"));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.expire().toString())
                .build();
    }

}
