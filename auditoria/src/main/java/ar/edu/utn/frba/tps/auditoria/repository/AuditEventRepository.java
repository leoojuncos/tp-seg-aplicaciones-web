package ar.edu.utn.frba.tps.auditoria.repository;

import ar.edu.utn.frba.tps.auditoria.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
}
