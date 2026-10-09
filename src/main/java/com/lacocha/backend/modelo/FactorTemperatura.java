package com.lacocha.backend.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Cuanto se corrige la tasa de alimentacion segun la temperatura del agua. */
@Entity
@Table(name = "factores_temperatura")
public class FactorTemperatura {

    @Id
    private Integer orden;
    @Column(name = "temp_hasta_c")
    private Double tempHastaC;
    /** 1.0 es la racion completa, 0.0 suspende la alimentacion. */
    private Double factor;
    private String fuente;

    public Integer getOrden() { return orden; }
    public Double getTempHastaC() { return tempHastaC; }
    public Double getFactor() { return factor; }
    public String getFuente() { return fuente; }
}
