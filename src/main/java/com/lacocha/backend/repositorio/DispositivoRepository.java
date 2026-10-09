package com.lacocha.backend.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.lacocha.backend.modelo.Dispositivo;

public interface DispositivoRepository extends JpaRepository<Dispositivo, String> {

    List<Dispositivo> findAllByOrderByUltimoVistoEnDesc();
}
