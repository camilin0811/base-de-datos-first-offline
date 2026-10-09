package com.lacocha.backend.dto;

import java.time.Instant;
import java.util.List;

import com.lacocha.backend.modelo.FactorTemperatura;
import com.lacocha.backend.modelo.ParametroRango;
import com.lacocha.backend.modelo.TasaAlimentacion;

/** Lo que usa el sistema experto para decidir, con la fuente de cada cifra. */
public final class Parametros {

    private Parametros() {
    }

    public record RangoSalida(String variable, String nombre, String unidad,
            Double optimoMin, Double optimoMax, Double criticoMin, Double criticoMax,
            String fuente, Instant actualizadoEn) {

        public static RangoSalida de(ParametroRango p) {
            return new RangoSalida(p.getVariable(), p.getNombre(), p.getUnidad(),
                    p.getOptimoMin(), p.getOptimoMax(), p.getCriticoMin(), p.getCriticoMax(),
                    p.getFuente(), p.getActualizadoEn());
        }
    }

    public record TasaSalida(Integer orden, Double pesoHastaG, Double tasaPct, String fuente) {

        public static TasaSalida de(TasaAlimentacion t) {
            return new TasaSalida(t.getOrden(), t.getPesoHastaG(), t.getTasaPct(), t.getFuente());
        }
    }

    public record FactorSalida(Integer orden, Double tempHastaC, Double factor, String fuente) {

        public static FactorSalida de(FactorTemperatura f) {
            return new FactorSalida(f.getOrden(), f.getTempHastaC(), f.getFactor(), f.getFuente());
        }
    }

    public record ParametrosSalida(List<RangoSalida> rangos, List<TasaSalida> tasasAlimentacion,
            List<FactorSalida> factoresTemperatura) {
    }
}
