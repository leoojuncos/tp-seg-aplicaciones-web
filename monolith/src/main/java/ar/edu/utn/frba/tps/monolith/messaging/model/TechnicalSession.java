package ar.edu.utn.frba.tps.monolith.messaging.model;

import java.time.Instant;

/** Sesion propia del modulo, emitida a una cuenta tecnica. Vive en memoria hasta que vence. */
public record TechnicalSession(String token, String username, TechnicalRole role, Instant expiresAt) {

    public boolean isExpiredAt(Instant instant) {
        return !instant.isBefore(expiresAt);
    }

}
