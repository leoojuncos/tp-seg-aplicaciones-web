package ar.edu.utn.frba.tps.monolith.tesoreria.dto;

import ar.edu.utn.frba.tps.monolith.tesoreria.model.DebtStatus;

import java.math.BigDecimal;

public record DebtResponse(Long id, String cuit, BigDecimal amount, DebtStatus status) {
}
