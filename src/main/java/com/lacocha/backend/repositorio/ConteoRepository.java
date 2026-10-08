package com.lacocha.backend.repositorio;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import com.lacocha.backend.modelo.Conteo;

public interface ConteoRepository extends JpaRepository<Conteo, UUID> {

    List<Conteo> findByLoteIdOrderByRegistradoEnAsc(UUID loteId);
}
