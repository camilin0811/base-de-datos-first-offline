package com.lacocha.backend.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Que porcentaje de la biomasa se da por dia segun cuanto pesa el pez. */
@Entity
@Table(name = "tasas_alimentacion")
public class TasaAlimentacion {

    @Id
    private Integer orden;
    /** Peso maximo del pez al que aplica la fila. Null en la ultima: de ahi para arriba. */
    @Column(name = "peso_hasta_g")
    private Double pesoHastaG;
    private Double tasaPct;
    private String fuente;

    public Integer getOrden() { return orden; }
    public Double getPesoHastaG() { return pesoHastaG; }
    public Double getTasaPct() { return tasaPct; }
    public String getFuente() { return fuente; }
}
