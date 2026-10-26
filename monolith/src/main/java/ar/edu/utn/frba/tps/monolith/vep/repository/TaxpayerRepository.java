package ar.edu.utn.frba.tps.monolith.vep.repository;

import ar.edu.utn.frba.tps.monolith.vep.model.Taxpayer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxpayerRepository extends JpaRepository<Taxpayer, String> {
}
