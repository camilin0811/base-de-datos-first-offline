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
}

