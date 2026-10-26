package ar.edu.utn.frba.tps.monolith.vep.service;

import ar.edu.utn.frba.tps.monolith.exception.NotFoundException;
import ar.edu.utn.frba.tps.monolith.exception.UnauthorizedException;
import ar.edu.utn.frba.tps.monolith.tesoreria.repository.DebtRepository;
import ar.edu.utn.frba.tps.monolith.vep.dto.VepDebtResponse;
import ar.edu.utn.frba.tps.monolith.vep.dto.VepTaxpayerResponse;
import ar.edu.utn.frba.tps.monolith.vep.mapper.VepMapper;
import ar.edu.utn.frba.tps.monolith.vep.model.Taxpayer;
import ar.edu.utn.frba.tps.monolith.vep.repository.TaxpayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class VepService {

    private final DebtRepository debtRepository;
    private final TaxpayerRepository taxpayerRepository;
    private final PasswordEncoder passwordEncoder;
    private final VepMapper mapper;
    private final String unknownTaxpayerHash;

    @Autowired
    public VepService(DebtRepository debtRepository, TaxpayerRepository taxpayerRepository,
                      PasswordEncoder passwordEncoder, VepMapper mapper) {
        this.debtRepository = debtRepository;
        this.taxpayerRepository = taxpayerRepository;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
        this.unknownTaxpayerHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    /**
     * El ingreso al portal: el contribuyente si la clave coincide con la de su CUIT. Si el CUIT no tiene
     * clave o la clave no coincide, el mismo 401, para no revelar que CUIT tiene cuenta.
     */
    public VepTaxpayerResponse login(String cuit, String password) {
        Optional<Taxpayer> taxpayer = taxpayerRepository.findById(cuit);
        // Se compara contra un hash aunque el CUIT no exista, para que el tiempo de respuesta tampoco lo
        // revele (como el login de messaging).
        String hash = taxpayer.map(Taxpayer::getPasswordHash).orElse(unknownTaxpayerHash);
        if (!passwordEncoder.matches(password, hash) || taxpayer.isEmpty()) {
            throw new UnauthorizedException("CUIT o clave incorrectos");
        }
        return mapper.toResponse(taxpayer.get());
    }

    /**
     * La deuda del CUIT con su estado actual: una condonada conserva su importe original. Hay una deuda
     * por CUIT (UNIQUE en debts), asi que findByCuit trae una o ninguna.
     */
    public VepDebtResponse get(String cuit) {
        return debtRepository.findByCuit(cuit).stream()
                .findFirst()
                .map(mapper::toResponse)
                .orElseThrow(() -> new NotFoundException("No hay deuda para el CUIT " + cuit));
    }

}
