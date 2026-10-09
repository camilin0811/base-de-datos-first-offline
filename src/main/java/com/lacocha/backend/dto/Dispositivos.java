package com.lacocha.backend.dto;

import java.time.Instant;

import com.lacocha.backend.modelo.Dispositivo;

public final class Dispositivos {

    private Dispositivos() {
    }

    public record DispositivoSalida(String id, String descripcion, boolean activo,
            Instant primerVistoEn, Instant ultimoVistoEn) {

        public static DispositivoSalida de(Dispositivo d) {
            return new DispositivoSalida(d.getId(), d.getDescripcion(), d.isActivo(),
                    d.getPrimerVistoEn(), d.getUltimoVistoEn());
        }
    }
}
