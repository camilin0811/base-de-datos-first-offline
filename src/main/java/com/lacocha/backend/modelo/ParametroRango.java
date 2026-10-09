package com.lacocha.backend.modelo;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Umbral de una variable de calidad de agua. Fuera del rango optimo se genera una
 * advertencia; fuera del critico, una alerta critica.
 *
 * Los maximos en null son variables sin tope por arriba, como el oxigeno disuelto.
 */
@Entity
@Table(name = "parametros_rango")
public class ParametroRango {

    @Id
    private String variable;
    private String nombre;
    /** Para armar "Temperatura alta" y no "Temperatura alto". */
    private boolean femenino;
    private String unidad;
    private Double optimoMin;
    private Double optimoMax;
    private Double criticoMin;
    private Double criticoMax;
    /** De donde salio el valor. Sin esto el umbral no es defendible. */
    private String fuente;
    private Instant actualizadoEn;

    public String getVariable() { return variable; }
    public String getNombre() { return nombre; }
    public boolean isFemenino() { return femenino; }
    public String getUnidad() { return unidad; }
    public Double getOptimoMin() { return optimoMin; }
    public Double getOptimoMax() { return optimoMax; }
    public Double getCriticoMin() { return criticoMin; }
    public Double getCriticoMax() { return criticoMax; }
    public String getFuente() { return fuente; }
    public Instant getActualizadoEn() { return actualizadoEn; }
}
