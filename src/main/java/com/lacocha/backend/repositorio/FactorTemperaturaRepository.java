package com.lacocha.backend.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.lacocha.backend.modelo.FactorTemperatura;

public interface FactorTemperaturaRepository extends JpaRepository<FactorTemperatura, Integer> {

    List<FactorTemperatura> findAllByOrderByOrdenAsc();
}
