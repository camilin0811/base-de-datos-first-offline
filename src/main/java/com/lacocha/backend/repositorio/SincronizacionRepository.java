package com.lacocha.backend.repositorio;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import com.lacocha.backend.modelo.Sincronizacion;

public interface SincronizacionRepository extends JpaRepository<Sincronizacion, UUID> {

    List<Sincronizacion> findTop200ByOrderByServidorEnDesc();

    List<Sincronizacion> findTop200ByDispositivoIdOrderByServidorEnDesc(String dispositivoId);
}
