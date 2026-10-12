package ar.edu.utn.frba.tps.monolith.vep.dto;

import ar.edu.utn.frba.tps.monolith.tesoreria.model.DebtStatus;

import java.math.BigDecimal;

/** La deuda tal como la muestra el portal publico: sin el id interno, que solo usa Tesoreria. */
public record VepDebtResponse(String cuit, BigDecimal amount, DebtStatus status) {
}
