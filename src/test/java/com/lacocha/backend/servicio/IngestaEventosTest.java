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

    /** Umbral fijo: esta prueba es de la tubería, no de qué es buena agua para una trucha. */

    private static final Reglas REGLAS = new Reglas(null, null, null) {
        @Override
        public List<Resultado> evaluarLectura(Double tempC, Double ph, Double oxigenoMgL) {
            if (tempC == null || tempC <= 18.0) {
                return List.of();
            }
            return List.of(new Resultado("temp_c", tempC, "critica",
                    "Temperatura alta: " + tempC + " °C (óptimo 10–16 °C)"));
        }
    };

    /**
     * Configurado como el que Spring Boot le entrega a la aplicación: la misma estrategia de
     * nombres que fija application.properties (sin ella "temp_c" no mapearía) y las propiedades
     * desconocidas ignoradas, porque cada trama trae un campo "tipo" que ningún record declara.
     */

    private final ObjectMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private final AlmacenEnMemoria almacen = new AlmacenEnMemoria();

    private final IngestoresEventos ingestores = new IngestoresEventos(REGLAS);

    private final IngestaEventos ingesta = new IngestaEventos(almacen,
            new AdaptadorJsonPush(mapper, Validation.buildDefaultValidatorFactory().getValidator(), ingestores),
            ingestores);

    private static final Instant SERVIDOR_EN = Instant.parse("2026-10-18T21:00:00Z");

    private static final String REGISTRADO_EN = "2026-10-18T15:30:00-05:00";

    private final UUID loteId = UUID.randomUUID();

    private final UUID estanqueId = UUID.randomUUID();

    private JsonNode json(String crudo) {
        try {
            return mapper.readTree(crudo);
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    private JsonNode conteo(UUID id, int total) {
        return json("""
                {"tipo":"conteo","id":"%s","registrado_en":"%s","origen":"contador",
                 "lote_id":"%s","total":%d,"cortes_multiples":2}
                """.formatted(id, REGISTRADO_EN, loteId, total));
    }

    private JsonNode lectura(UUID id, double tempC) {
        return json("""
                {"tipo":"lectura_agua","id":"%s","registrado_en":"%s","origen":"sensor",
                 "estanque_id":"%s","temp_c":%s}
                """.formatted(id, REGISTRADO_EN, estanqueId, tempC));
    }

    private IngestaEventos.Resultado ingerir(JsonNode... tramas) {
        return ingesta.ingerir(List.of(tramas), "cel-prueba", SERVIDOR_EN);
    }

    @Test
    void aceptaUnConteoYLoSellaConElDispositivoYLaHoraDeLlegada() {
        almacen.padres.add(loteId);
        UUID id = UUID.randomUUID();

        IngestaEventos.Resultado r = ingerir(conteo(id, 1200));

        assertThat(r.aceptados()).containsExactly(id);
        assertThat(r.rechazados()).isEmpty();
        Conteo guardado = (Conteo) almacen.guardados.values().iterator().next();
        assertThat(guardado.getTotal()).isEqualTo(1200);
        assertThat(guardado.getCortesMultiples()).isEqualTo(2);
        assertThat(guardado.getOrigen()).isEqualTo("contador");
        assertThat(guardado.getDispositivoId()).isEqualTo("cel-prueba");
        assertThat(guardado.getRecibidoEn()).isEqualTo(SERVIDOR_EN);
        assertThat(guardado.getRegistradoEn()).isEqualTo(OffsetDateTime.parse(REGISTRADO_EN).toInstant());
    }

    @Test
    void elMismoEventoDosVecesEsDuplicadoYSeGuardaUnaVez() {
        almacen.padres.add(loteId);
        UUID id = UUID.randomUUID();

        assertThat(ingerir(conteo(id, 500)).aceptados()).containsExactly(id);
        IngestaEventos.Resultado reintento = ingerir(conteo(id, 500));

        assertThat(reintento.duplicados()).containsExactly(id);
        assertThat(reintento.aceptados()).isEmpty();
        assertThat(almacen.guardados).hasSize(1);
    }

    @Test
    void unEventoHuerfanoSeRechazaSoloYElRestoDeLaColaPasa() {
        almacen.padres.add(loteId);
        UUID bueno = UUID.randomUUID();
        UUID huerfano = UUID.randomUUID();
        JsonNode otroLote = json("""
                {"tipo":"conteo","id":"%s","registrado_en":"%s","lote_id":"%s","total":10}
                """.formatted(huerfano, REGISTRADO_EN, UUID.randomUUID()));

        IngestaEventos.Resultado r = ingerir(otroLote, conteo(bueno, 10));

        assertThat(r.aceptados()).containsExactly(bueno);
        assertThat(r.rechazados()).singleElement().satisfies(rechazo -> {
            assertThat(rechazo.id()).isEqualTo(huerfano.toString());
            assertThat(rechazo.error()).isEqualTo("lote_id: no existe");
        });
    }

    @Test
    void elRechazoNombraElCampoComoLoNombraElJson() {
        almacen.padres.add(loteId);
        JsonNode negativo = json("""
                {"tipo":"conteo","id":"%s","registrado_en":"%s","lote_id":"%s","total":-5}
                """.formatted(UUID.randomUUID(), REGISTRADO_EN, loteId));

        assertThat(ingerir(negativo).rechazados()).singleElement()
                .satisfies(rechazo -> assertThat(rechazo.error()).startsWith("total:"));
    }

    @Test
    void unTipoDesconocidoListaLosQueElServidorAcepta() {
        JsonNode desconocido = json("""
                {"tipo":"caudal","id":"%s","registrado_en":"%s"}
                """.formatted(UUID.randomUUID(), REGISTRADO_EN));

        assertThat(ingerir(desconocido).rechazados()).singleElement()
                .satisfies(rechazo -> assertThat(rechazo.error())
                        .isEqualTo("tipo: debe ser uno de lectura_agua, conteo, mortalidad, alimentacion, biometria"));
    }

    @Test
    void sinOrigenElEventoSeGuardaComoManual() {
        almacen.padres.add(loteId);
        JsonNode sinOrigen = json("""
                {"tipo":"conteo","id":"%s","registrado_en":"%s","lote_id":"%s","total":7}
                """.formatted(UUID.randomUUID(), REGISTRADO_EN, loteId));

        ingerir(sinOrigen);

        assertThat(almacen.guardados.values()).singleElement()
                .satisfies(e -> assertThat(e.getOrigen()).isEqualTo("manual"));
    }

    @Test
    void unaLecturaFueraDeRangoLevantaAlerta() {
        almacen.padres.add(estanqueId);

        IngestaEventos.Resultado r = ingerir(lectura(UUID.randomUUID(), 22.0));

        assertThat(r.alertasGeneradas()).isEqualTo(1);
        assertThat(almacen.alertas).singleElement().satisfies(a -> {
            assertThat(a.getVariable()).isEqualTo("temp_c");
            assertThat(a.getNivel()).isEqualTo("critica");
            assertThat(a.getEstanqueId()).isEqualTo(estanqueId);
        });
    }

    /**
     * Este backend no descarta una alerta igual que siga sin atender: cada lectura fuera de rango
     * levanta la suya. Queda escrito aquí para que el día que se agregue ese filtro la prueba lo
     * diga en voz alta, en vez de que el cambio pase sin que nadie lo note.
     */
}

