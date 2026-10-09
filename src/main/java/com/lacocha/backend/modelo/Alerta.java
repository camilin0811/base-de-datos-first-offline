package com.lacocha.backend.modelo;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * La genera el servidor. Hoy nacen de una lectura de agua fuera de rango, pero la tabla ya
 * admite alertas de lote (mortalidad, supervivencia): en esas, lectura_id y variable van
 * nulas y lote_id trae el lote.
 */
@Entity
@Table(name = "alertas")
public class Alerta {

    @Id
    private UUID id = UUID.randomUUID();
    private UUID estanqueId;
    private UUID lecturaId;
    private UUID loteId;
    /** lectura_agua | mortalidad | supervivencia */
    private String disparadaPor = "lectura_agua";
    private String variable;
    private Double valor;
    private String nivel; // advertencia | critica
    private String mensaje;
    private Instant medidoEn;
    private Instant creadaEn = Instant.now();
    private boolean atendida = false;
    private Instant atendidaEn;
    /** Id del dispositivo si la atendio el celular, o "panel" si fue el navegador. */
    private String atendidaPor;

    public UUID getId() { return id; }
    public UUID getEstanqueId() { return estanqueId; }
    public void setEstanqueId(UUID estanqueId) { this.estanqueId = estanqueId; }
    public UUID getLecturaId() { return lecturaId; }
    public void setLecturaId(UUID lecturaId) { this.lecturaId = lecturaId; }
    public UUID getLoteId() { return loteId; }
    public void setLoteId(UUID loteId) { this.loteId = loteId; }
    public String getDisparadaPor() { return disparadaPor; }
    public void setDisparadaPor(String disparadaPor) { this.disparadaPor = disparadaPor; }
    public String getVariable() { return variable; }
    public void setVariable(String variable) { this.variable = variable; }
    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }
    public String getNivel() { return nivel; }
    public void setNivel(String nivel) { this.nivel = nivel; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public Instant getMedidoEn() { return medidoEn; }
    public void setMedidoEn(Instant medidoEn) { this.medidoEn = medidoEn; }
    public Instant getCreadaEn() { return creadaEn; }
    public boolean isAtendida() { return atendida; }
    public void setAtendida(boolean atendida) { this.atendida = atendida; }
    public Instant getAtendidaEn() { return atendidaEn; }
    public void setAtendidaEn(Instant atendidaEn) { this.atendidaEn = atendidaEn; }
    public String getAtendidaPor() { return atendidaPor; }
    public void setAtendidaPor(String atendidaPor) { this.atendidaPor = atendidaPor; }
}
