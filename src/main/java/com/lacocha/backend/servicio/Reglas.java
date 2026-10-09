package com.lacocha.backend.servicio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.lacocha.backend.modelo.FactorTemperatura;
import com.lacocha.backend.modelo.ParametroRango;
import com.lacocha.backend.modelo.TasaAlimentacion;
import com.lacocha.backend.repositorio.FactorTemperaturaRepository;
import com.lacocha.backend.repositorio.ParametroRangoRepository;
import com.lacocha.backend.repositorio.TasaAlimentacionRepository;

/**
 * Sistema experto (nivel 1 de la IA): umbrales de calidad de agua y racion de alimento.
 *
 * Los valores ya no estan compilados aca: salen de las tablas parametros_rango,
 * tasas_alimentacion y factores_temperatura, cada uno con su fuente. Ajustarlos con el
 * productor pasa a ser un UPDATE y no un redespliegue.
 */
@Service
public class Reglas {

    private static final double SIN_TOPE = Double.POSITIVE_INFINITY;

    /** Orden en que se evaluan las variables, que es el orden en que salen las alertas. */
    private static final List<String> VARIABLES = List.of("temp_c", "ph", "oxigeno_mg_l");

    private final ParametroRangoRepository parametros;
    private final TasaAlimentacionRepository tasas;
    private final FactorTemperaturaRepository factores;

    /**
     * Las tres tablas se consultan en cada lectura evaluada y cambian muy de vez en cuando,
     * asi que se guardan en memoria. Un push puede traer 500 eventos: sin esto serian 1.500
     * consultas por envio.
     *
     * Si dos hilos la cargan a la vez leen lo mismo de la base y el resultado es equivalente,
     * asi que no hace falta sincronizar.
     */
    private volatile Tablas cache;

    public Reglas(ParametroRangoRepository parametros, TasaAlimentacionRepository tasas,
            FactorTemperaturaRepository factores) {
        this.parametros = parametros;
        this.tasas = tasas;
        this.factores = factores;
    }

    private record Rango(String variable, String nombre, boolean femenino, String unidad,
            double optimoMin, double optimoMax, double criticoMin, double criticoMax) {
    }

    /** Fila de una curva: aplica hasta "hasta" inclusive y vale "valor". */
    private record Tramo(double hasta, double valor) {
    }

    private record Tablas(Map<String, Rango> rangos, List<Tramo> tasas, List<Tramo> factores) {
    }

    public record Resultado(String variable, double valor, String nivel, String mensaje) {
    }

    /** Vuelve a leer las tablas. Se llama cuando se cambia un umbral desde el panel. */
    public void recargar() {
        cache = null;
    }

    private Tablas tablas() {
        Tablas actual = cache;
        if (actual == null) {
            actual = cargar();
            cache = actual;
        }
        return actual;
    }

    private Tablas cargar() {
        Map<String, Rango> rangos = new LinkedHashMap<>();
        for (ParametroRango p : parametros.findAllByOrderByVariableAsc()) {
            rangos.put(p.getVariable(), new Rango(p.getVariable(), p.getNombre(), p.isFemenino(),
                    p.getUnidad(), p.getOptimoMin(), sinTope(p.getOptimoMax()),
                    p.getCriticoMin(), sinTope(p.getCriticoMax())));
        }

        List<Tramo> curvaTasas = new ArrayList<>();
        for (TasaAlimentacion t : tasas.findAllByOrderByOrdenAsc()) {
            curvaTasas.add(new Tramo(sinTope(t.getPesoHastaG()), t.getTasaPct()));
        }

        List<Tramo> curvaFactores = new ArrayList<>();
        for (FactorTemperatura f : factores.findAllByOrderByOrdenAsc()) {
            curvaFactores.add(new Tramo(sinTope(f.getTempHastaC()), f.getFactor()));
        }
        return new Tablas(rangos, curvaTasas, curvaFactores);
    }

    /** En la base, "sin tope por arriba" se guarda como NULL. */
    private static double sinTope(Double valor) {
        return valor != null ? valor : SIN_TOPE;
    }

    public List<Resultado> evaluarLectura(Double tempC, Double ph, Double oxigenoMgL) {
        Tablas t = tablas();
        Map<String, Double> medidos = new LinkedHashMap<>();
        medidos.put("temp_c", tempC);
        medidos.put("ph", ph);
        medidos.put("oxigeno_mg_l", oxigenoMgL);

        List<Resultado> resultados = new ArrayList<>();
        for (String variable : VARIABLES) {
            // Si la variable no tiene fila en la tabla no se evalua: quitarsela es la forma
            // de apagar sus alertas sin tocar el codigo.
            Rango rango = t.rangos().get(variable);
            Double valor = medidos.get(variable);
            if (rango == null || valor == null || (valor >= rango.optimoMin() && valor <= rango.optimoMax())) {
                continue;
            }
            boolean critico = valor < rango.criticoMin() || valor > rango.criticoMax();
            String direccion = (valor > rango.optimoMax() ? "alt" : "baj") + (rango.femenino() ? "a" : "o");
            String optimo = rango.optimoMax() == SIN_TOPE
                    ? "mínimo " + fmt(rango.optimoMin()) + rango.unidad()
                    : "óptimo " + fmt(rango.optimoMin()) + "–" + fmt(rango.optimoMax()) + rango.unidad();
            resultados.add(new Resultado(rango.variable(), valor, critico ? "critica" : "advertencia",
                    rango.nombre() + " " + direccion + ": " + fmt(valor) + rango.unidad() + " (" + optimo + ")"));
        }
        return resultados;
    }

    public double tasaAlimentacionPct(double pesoG, double tempC) {
        Tablas t = tablas();
        return redondear(buscar(t.tasas(), pesoG) * buscar(t.factores(), tempC), 2);
    }

    public double racionDiariaKg(double biomasaKg, double pesoG, double tempC) {
        return redondear(biomasaKg * tasaAlimentacionPct(pesoG, tempC) / 100, 3);
    }

    private static double buscar(List<Tramo> curva, double x) {
        for (Tramo tramo : curva) {
            if (x <= tramo.hasta()) {
                return tramo.valor();
            }
        }
        // Pasa si la curva quedo vacia o si a la ultima fila le pusieron un tope. Devolver
        // cero es lo prudente: antes que recomendar una racion inventada, no recomendar nada.
        return 0;
    }

    static double redondear(double x, int decimales) {
        double factor = Math.pow(10, decimales);
        return Math.round(x * factor) / factor;
    }

    /** 19.0 -> "19", 6.25 -> "6.25" (siempre con punto, sin importar el idioma del servidor). */
    private static String fmt(double x) {
        return BigDecimal.valueOf(x).stripTrailingZeros().toPlainString();
    }
}
