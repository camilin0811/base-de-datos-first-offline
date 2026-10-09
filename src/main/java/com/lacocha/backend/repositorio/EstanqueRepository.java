package com.lacocha.backend.repositorio;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import com.lacocha.backend.modelo.Estanque;

public interface EstanqueRepository extends JpaRepository<Estanque, UUID> {

    List<Estanque> findAllByOrderByNombreAsc();

    Optional<Estanque> findByNombre(String nombre);

    List<Estanque> findByServidorEnAfter(Instant desde);
}
