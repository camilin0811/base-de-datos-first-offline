package com.lacocha.backend.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.lacocha.backend.modelo.TasaAlimentacion;

public interface TasaAlimentacionRepository extends JpaRepository<TasaAlimentacion, Integer> {

    List<TasaAlimentacion> findAllByOrderByOrdenAsc();
}
