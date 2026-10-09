package com.lacocha.backend.modelo;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Celular o nodo que sincroniza. El id es el mismo texto que la app manda en dispositivo_id,
 * no un UUID generado aqui, para que los eventos ya guardados puedan apuntar a esta tabla.
 */
@Entity
@Table(name = "dispositivos")
public class Dispositivo {

    @Id
    private String id;
    /** De quien es el celular, para que el productor sepa cual es cual. */
    private String descripcion;
    private boolean activo = true;
    private Instant primerVistoEn;
    /** Se actualiza en cada push: distingue un celular sin señal de uno perdido. */
    private Instant ultimoVistoEn;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public Instant getPrimerVistoEn() { return primerVistoEn; }
    public void setPrimerVistoEn(Instant primerVistoEn) { this.primerVistoEn = primerVistoEn; }
    public Instant getUltimoVistoEn() { return ultimoVistoEn; }
    public void setUltimoVistoEn(Instant ultimoVistoEn) { this.ultimoVistoEn = ultimoVistoEn; }
}
