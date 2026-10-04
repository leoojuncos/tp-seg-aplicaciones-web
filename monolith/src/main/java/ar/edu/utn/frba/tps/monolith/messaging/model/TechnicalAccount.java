package ar.edu.utn.frba.tps.monolith.messaging.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "technical_accounts")
public class TechnicalAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private TechnicalRole role;

    @Column(name = "service_id")
    private String serviceId;

    protected TechnicalAccount() {
    }

    public TechnicalAccount(String username, String passwordHash, TechnicalRole role, String serviceId) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.serviceId = serviceId;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public TechnicalRole getRole() {
        return role;
    }

    public String getServiceId() {
        return serviceId;
    }

}
