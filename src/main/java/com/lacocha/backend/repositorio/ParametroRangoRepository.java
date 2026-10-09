package com.lacocha.backend.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.lacocha.backend.modelo.ParametroRango;

public interface ParametroRangoRepository extends JpaRepository<ParametroRango, String> {

    List<ParametroRango> findAllByOrderByVariableAsc();
}
