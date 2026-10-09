package com.lacocha.backend.dto;

import java.time.Instant;

import com.lacocha.backend.modelo.Dispositivo;
import com.lacocha.backend.modelo.Sincronizacion;

import jakarta.validation.constraints.Size;

public final class Dispositivos {

    private Dispositivos() {
    }

    public record SincronizacionSalida(java.util.UUID id, String dispositivoId, int aceptados,
            int duplicados, int obsoletos, int rechazados, int alertasGeneradas, Instant servidorEn) {

        public static SincronizacionSalida de(Sincronizacion s) {
            return new SincronizacionSalida(s.getId(), s.getDispositivoId(), s.getAceptados(),
                    s.getDuplicados(), s.getObsoletos(), s.getRechazados(), s.getAlertasGeneradas(),
                    s.getServidorEn());
        }
    }

    public record DispositivoEditar(
            @Size(min = 1, max = 120) String descripcion,
            Boolean activo) {
    }

    public record DispositivoSalida(String id, String descripcion, boolean activo,
            Instant primerVistoEn, Instant ultimoVistoEn) {

        public static DispositivoSalida de(Dispositivo d) {
            return new DispositivoSalida(d.getId(), d.getDescripcion(), d.isActivo(),
                    d.getPrimerVistoEn(), d.getUltimoVistoEn());
        }
    }
}
