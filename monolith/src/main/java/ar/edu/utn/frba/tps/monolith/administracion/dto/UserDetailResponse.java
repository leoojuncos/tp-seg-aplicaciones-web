package ar.edu.utn.frba.tps.monolith.administracion.dto;

import java.util.List;

public record UserDetailResponse(Long id, String username, String role, List<String> permissions) {
}
