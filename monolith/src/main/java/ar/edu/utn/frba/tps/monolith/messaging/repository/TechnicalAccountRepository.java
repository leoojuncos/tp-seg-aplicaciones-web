package ar.edu.utn.frba.tps.monolith.messaging.repository;

import ar.edu.utn.frba.tps.monolith.messaging.model.TechnicalAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TechnicalAccountRepository extends JpaRepository<TechnicalAccount, Long> {

    Optional<TechnicalAccount> findByUsername(String username);

}
