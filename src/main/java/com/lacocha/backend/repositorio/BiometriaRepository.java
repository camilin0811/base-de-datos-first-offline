package com.lacocha.backend.repositorio;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import com.lacocha.backend.modelo.Biometria;

public interface BiometriaRepository extends JpaRepository<Biometria, UUID> {

    Optional<Biometria> findFirstByLoteIdOrderByRegistradoEnDesc(UUID loteId);
}
