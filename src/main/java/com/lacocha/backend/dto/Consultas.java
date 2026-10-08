package com.lacocha.backend.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.lacocha.backend.modelo.LecturaAgua;

public final class Consultas {

    private Consultas() {
    }

    public record LecturaAguaSalida(UUID id, UUID estanqueId, Double tempC, Double ph, Double oxigenoMgL,
            String origen, Instant registradoEn) {

        public static LecturaAguaSalida de(LecturaAgua l) {
            return new LecturaAguaSalida(l.getId(), l.getEstanqueId(), l.getTempC(), l.getPh(), l.getOxigenoMgL(),
                    l.getOrigen(), l.getRegistradoEn());
        }
    }

    public record ResumenLote(
            UUID loteId,
            String codigo,
            UUID estanqueId,
            Integer cantidadInicial,
            Integer poblacionEstimada,
            long mortalidadTotal,
            Double supervivenciaPct,
            Double pesoPromedioG,
            Double biomasaKg,
            Double ultimaTemperaturaC,
            Instant ultimaLecturaEn,
            Double tasaAlimentacionPct,
            Double racionDiariaKg,
            double alimentoUltimaSemanaKg,
            long alertasPendientes,
            List<String> notas) {
    }
}
