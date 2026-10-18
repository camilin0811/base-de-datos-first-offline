package com.lacocha.backend.servicio;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lacocha.backend.modelo.Alerta;
import com.lacocha.backend.modelo.Evento;

/**
 * Capacidades transversales del almacén de eventos, agregadas desde fuera y apilables. Ni la
 * tubería de ingesta ni el almacén JPA saben que están ahí, y apagar una es no envolverla.
 */
public final class DecoradoresAlmacenEventos {

    private DecoradoresAlmacenEventos() {
    }

    /** Base: un AlmacenEventos que envuelve a otro y reenvía todo sin tocarlo. */
    public abstract static class Decorador implements AlmacenEventos {

        protected final AlmacenEventos interno;

        protected Decorador(AlmacenEventos interno) {
            this.interno = interno;
        }

        @Override
        public boolean existe(Evento evento) {
            return interno.existe(evento);
        }

        @Override
        public void guardar(Evento evento) {
            interno.guardar(evento);
        }

        @Override
        public boolean existePadre(boolean padreEsEstanque, UUID padreId) {
            return interno.existePadre(padreEsEstanque, padreId);
        }

        @Override
        public int guardarAlertas(List<Alerta> alertas) {
            return interno.guardarAlertas(alertas);
        }
    }

    /**
     * Una línea por evento guardado. Un push trae hasta 500, así que solo se envuelve cuando su
     * nivel está realmente encendido (ver encendido()). Existe para responder la pregunta que
     * siempre sale en campo: "la app dice que lo subió, ¿está o no está?".
     */
    public static final class Rastro extends Decorador {

        private static final Logger log = LoggerFactory.getLogger(Rastro.class);

        public Rastro(AlmacenEventos interno) {
            super(interno);
        }

        /** Permite que la tubería no envuelva nada cuando nadie va a leer el rastro. */
        public static boolean encendido() {
            return log.isDebugEnabled();
        }

        @Override
        public void guardar(Evento evento) {
            interno.guardar(evento);
            log.debug("guardado {} {} de {} (origen {})", evento.getClass().getSimpleName(),
                    evento.getId(), evento.getDispositivoId(), evento.getOrigen());
        }
    }

    /**
     * Cuenta lo guardado por origen. La tesis compara el contador de alevinos contra el conteo a
     * mano, y esto responde cuánto de lo que llegó lo midió la máquina sin tener que consultar la
     * base. Vive un solo push: una cuenta que sumara dos pushes distintos no diría nada.
     */
    public static final class ConteoPorOrigen extends Decorador {

        private final Map<String, Integer> porOrigen = new TreeMap<>();

        public ConteoPorOrigen(AlmacenEventos interno) {
            super(interno);
        }

        @Override
        public void guardar(Evento evento) {
            interno.guardar(evento);
            porOrigen.merge(evento.getOrigen(), 1, Integer::sum);
        }

        /** "contador=120, manual=3". Vacío cuando el push no trajo eventos. */
        public String resumen() {
            return porOrigen.entrySet().stream()
                    .map(entrada -> entrada.getKey() + "=" + entrada.getValue())
                    .collect(Collectors.joining(", "));
        }
    }
}
