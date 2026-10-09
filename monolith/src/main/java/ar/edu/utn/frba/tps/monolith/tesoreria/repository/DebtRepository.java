package ar.edu.utn.frba.tps.monolith.tesoreria.repository;

import ar.edu.utn.frba.tps.monolith.tesoreria.model.Debt;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface DebtRepository extends JpaRepository<Debt, Long> {

    List<Debt> findByCuit(String cuit);

    /**
     * Lee la deuda bloqueando su fila hasta el final de la transaccion, para que dos condonaciones
     * simultaneas no la encuentren las dos pendiente.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Debt> findWithLockById(Long id);

}
