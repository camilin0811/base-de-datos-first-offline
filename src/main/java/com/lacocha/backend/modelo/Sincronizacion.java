package com.lacocha.backend.modelo;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Lo que paso en un push. El id lo genera el servidor, no el celular: es el registro de la
 * llamada, no un dato del cultivo, y un reintento del celular es una sincronizacion nueva.
 */
@Entity
@Table(name = "sincronizaciones")
public class Sincronizacion {

    @Id
    private UUID id = UUID.randomUUID();
    private String dispositivoId;
    private int aceptados;
    /** Cada duplicado es un reintento que no duplico datos. */
    private int duplicados;
    private int obsoletos;
    private int rechazados;
    private int alertasGeneradas;
    private Instant servidorEn;

    public UUID getId() { return id; }
    public String getDispositivoId() { return dispositivoId; }
    public void setDispositivoId(String dispositivoId) { this.dispositivoId = dispositivoId; }
    public int getAceptados() { return aceptados; }
    public void setAceptados(int aceptados) { this.aceptados = aceptados; }
    public int getDuplicados() { return duplicados; }
    public void setDuplicados(int duplicados) { this.duplicados = duplicados; }
    public int getObsoletos() { return obsoletos; }
    public void setObsoletos(int obsoletos) { this.obsoletos = obsoletos; }
    public int getRechazados() { return rechazados; }
    public void setRechazados(int rechazados) { this.rechazados = rechazados; }
    public int getAlertasGeneradas() { return alertasGeneradas; }
    public void setAlertasGeneradas(int alertasGeneradas) { this.alertasGeneradas = alertasGeneradas; }
    public Instant getServidorEn() { return servidorEn; }
    public void setServidorEn(Instant servidorEn) { this.servidorEn = servidorEn; }
}
