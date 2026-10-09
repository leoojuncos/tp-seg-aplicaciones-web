package ar.edu.utn.frba.tps.monolith.tesoreria.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Deuda de un contribuyente, una por CUIT. Condonarla cambia el estado y conserva el importe original,
 * que el VEP muestra junto al estado.
 */
@Entity
@Table(name = "debts")
public class Debt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cuit;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private DebtStatus status;

    protected Debt() {
    }

    public Debt(String cuit, BigDecimal amount) {
        this.cuit = cuit;
        this.amount = amount;
        this.status = DebtStatus.PENDING;
    }

    public Long getId() {
        return id;
    }

    public String getCuit() {
        return cuit;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public DebtStatus getStatus() {
        return status;
    }

    public void forgive() {
        this.status = DebtStatus.FORGIVEN;
    }

}
