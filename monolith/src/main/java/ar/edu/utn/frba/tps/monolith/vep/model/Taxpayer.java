package ar.edu.utn.frba.tps.monolith.vep.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Contribuyente con clave para el portal: entra con su CUIT y ve la deuda de ese CUIT. */
@Entity
@Table(name = "taxpayers")
public class Taxpayer {

    @Id
    private String cuit;

    private String name;

    @Column(name = "password_hash")
    private String passwordHash;

    protected Taxpayer() {
    }

    public Taxpayer(String cuit, String name, String passwordHash) {
        this.cuit = cuit;
        this.name = name;
        this.passwordHash = passwordHash;
    }

    public String getCuit() {
        return cuit;
    }

    public String getName() {
        return name;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

}
