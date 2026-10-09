package com.lacocha.backend.repositorio;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import com.lacocha.backend.modelo.Lote;

public interface LoteRepository extends JpaRepository<Lote, UUID> {

    List<Lote> findByServidorEnAfter(Instant desde);

    Optional<Lote> findByEstanqueIdAndCodigo(UUID estanqueId, String codigo);
}
