package com.lacocha.backend.servicio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lacocha.backend.modelo.Alerta;
import com.lacocha.backend.modelo.Conteo;
import com.lacocha.backend.modelo.Evento;

import jakarta.validation.Validation;

/**
 * Las reglas del push, probadas sin base de datos y sin contexto de Spring.
 *
 * Eso es lo que compra el Bridge: IngestaEventos sostiene un AlmacenEventos y nunca sabe cuál es,
 * así que aquí el almacén es un mapa. Estas comprobaciones tardan milisegundos; el mismo terreno
 * recorrido por HTTP en ApiTest cuesta levantar la aplicación y el esquema de H2.
 */
class IngestaEventosTest {
    /** Implementador del Bridge para las pruebas. */

    static final class AlmacenEnMemoria implements AlmacenEventos {

        final Map<String, Evento> guardados = new LinkedHashMap<>();
        final List<Alerta> alertas = new ArrayList<>();
        final Set<UUID> padres = new HashSet<>();

        /** Cada clase de evento tiene su tabla, así que la llave es la tabla y el id, no el id solo. */
        private static String llave(Evento evento) {
            return evento.getClass().getSimpleName() + ":" + evento.getId();
        }

        @Override
        public boolean existe(Evento evento) {
            return guardados.containsKey(llave(evento));
        }

        @Override
        public void guardar(Evento evento) {
            guardados.put(llave(evento), evento);
        }

        @Override
        public boolean existePadre(boolean padreEsEstanque, UUID padreId) {
            return padres.contains(padreId);
        }

        @Override
        public int guardarAlertas(List<Alerta> candidatas) {
            alertas.addAll(candidatas);
            return candidatas.size();
        }
    }
}

