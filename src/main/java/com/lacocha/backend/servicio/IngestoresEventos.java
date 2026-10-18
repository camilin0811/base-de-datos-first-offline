package com.lacocha.backend.servicio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.lacocha.backend.dto.Eventos.AlimentacionEntrada;
import com.lacocha.backend.dto.Eventos.BiometriaEntrada;
import com.lacocha.backend.dto.Eventos.ConteoEntrada;
import com.lacocha.backend.dto.Eventos.Entrada;
import com.lacocha.backend.dto.Eventos.LecturaAguaEntrada;
import com.lacocha.backend.dto.Eventos.MortalidadEntrada;
import com.lacocha.backend.modelo.Alerta;

/**
 * Las clases de evento que acepta el servidor, y qué dispara cada una una vez guardada.
 *
 * Es la única lista de las etiquetas del protocolo: el adaptador pregunta aquí en qué record tiene
 * que leer una trama cruda, y la tubería pregunta aquí qué hacer con ella después.
 */
@Component
public class IngestoresEventos {
    /** Lo que dispara una clase de evento una vez guardada. Devuelve cuántas alertas creó. */

    public interface Ingestor {
        int despuesDeGuardar(Entrada evento, AlmacenEventos almacen);
    }

    /**
     * Método fábrica: cada subclase responde por un "tipo" del protocolo y decide qué ingestor lo
     * atiende. Aceptar una clase nueva de evento es agregar una subclase y registrarla más abajo;

     * el adaptador, la tubería y los controladores no cambian.
     */

    public abstract static class Fabrica {

        private Ingestor cache;

        /** La etiqueta que el dispositivo pone en el campo "tipo" del JSON. */
        public abstract String tipo();

        /** El record en el que el adaptador tiene que leer la trama cruda. */
        public abstract Class<? extends Entrada> claseEntrada();

        /** El método fábrica. */
        protected abstract Ingestor crearIngestor();

        /** Se crea una sola vez: un ingestor no guarda estado y un push trae hasta 500 eventos. */
        public final Ingestor ingestor() {
            if (cache == null) {
                cache = crearIngestor();
            }
            return cache;
        }
    }

    /** Los eventos que solo se guardan: conteo, mortalidad, alimentación y biometría. */

    public static final class Llano extends Fabrica {

        private final String tipo;
        private final Class<? extends Entrada> claseEntrada;

        public Llano(String tipo, Class<? extends Entrada> claseEntrada) {
            this.tipo = tipo;
            this.claseEntrada = claseEntrada;
        }

        @Override
        public String tipo() {
            return tipo;
        }

        @Override
        public Class<? extends Entrada> claseEntrada() {
            return claseEntrada;
        }

        @Override
        protected Ingestor crearIngestor() {
            return (evento, almacen) -> 0;
        }
    }

    /**
     * Una lectura de agua además corre el sistema experto. La alerta tiene que levantarse en la
     * misma transacción que guardó la lectura: si no, un push que se revierte dejaría una alerta
     * apuntando a una lectura que nadie tiene.
     */

    public static final class LecturasAgua extends Fabrica {

        private final Reglas reglas;

        public LecturasAgua(Reglas reglas) {
            this.reglas = reglas;
        }

        @Override
        public String tipo() {
            return "lectura_agua";
        }

        @Override
        public Class<? extends Entrada> claseEntrada() {
            return LecturaAguaEntrada.class;
        }

        @Override
        protected Ingestor crearIngestor() {
            return this::levantarAlertas;
        }

        private int levantarAlertas(Entrada evento, AlmacenEventos almacen) {
            LecturaAguaEntrada lectura = (LecturaAguaEntrada) evento;
            List<Alerta> candidatas = new ArrayList<>();
            for (Reglas.Resultado regla : reglas.evaluarLectura(lectura.tempC(), lectura.ph(),
                    lectura.oxigenoMgL())) {
                Alerta alerta = new Alerta();
                alerta.setEstanqueId(lectura.estanqueId());
                alerta.setLecturaId(lectura.id());
                alerta.setDisparadaPor("lectura_agua");
                alerta.setVariable(regla.variable());
                alerta.setValor(regla.valor());
                alerta.setNivel(regla.nivel());
                alerta.setMensaje(regla.mensaje());
                alerta.setMedidoEn(lectura.registradoEn().toInstant());
                candidatas.add(alerta);
            }
            return almacen.guardarAlertas(candidatas);
        }
    }

    /** LinkedHashMap para que el mensaje de error liste las etiquetas siempre en el mismo orden. */

    private final Map<String, Fabrica> porTipo = new LinkedHashMap<>();

    private final Map<Class<? extends Entrada>, Fabrica> porClase = new LinkedHashMap<>();

    public IngestoresEventos(Reglas reglas) {
        registrar(new LecturasAgua(reglas));
        registrar(new Llano("conteo", ConteoEntrada.class));
        registrar(new Llano("mortalidad", MortalidadEntrada.class));
        registrar(new Llano("alimentacion", AlimentacionEntrada.class));
        registrar(new Llano("biometria", BiometriaEntrada.class));
    }

    private void registrar(Fabrica fabrica) {
        porTipo.put(fabrica.tipo(), fabrica);
        porClase.put(fabrica.claseEntrada(), fabrica);
    }
}

