package com.lacocha.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Las restricciones de hallazgos_ia, comprobadas por SQL directo.
 *
 * Es el oficio de este repositorio. Las validaciones de los DTO protegen lo que entra por la API, y
 * aquí no entra nada por la API: se inserta a mano, igual que lo haría el panel web, un script de
 * carga o alguien con psql abierto. Si la regla solo vive en Java, cualquiera de esos tres la salta
 * sin enterarse, y lo que queda en la base es un hallazgo del que nadie puede decir de dónde salió.
 *
 * La V26 pone tres reglas y cada una tiene su prueba abajo.
 *
 * Los nombres de las restricciones se comparan en minúscula porque es como viajan en el mensaje:
 * H2 corre con DATABASE_TO_LOWER y PostgreSQL baja los identificadores sin comillas, así que los
 * dos motores dicen "ck_hallazgos_ia_nivel" y no la versión en mayúsculas.
 */
@SpringBootTest
class EsquemaHallazgosTest {

    @Autowired
    JdbcTemplate jdbc;

    private static final String INSERTAR = """
            insert into hallazgos_ia
                (id, estanque_id, lote_id, clase, nivel, variable, puntaje, mensaje, medido_en,
                 creado_en, atendido, atendido_en)
            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    /** Un estanque de verdad, porque estanque_id es llave foranea. */
    private UUID estanque() {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into estanques (id, nombre, tipo, activo, actualizado_en, servidor_en)
                values (?, ?, 'tanque', true, ?, ?)
                """, id, "Tanque " + id.toString().substring(0, 8), Instant.now(), Instant.now());
        return id;
    }

    private int insertar(UUID estanqueId, UUID loteId, String clase, String nivel, String variable,
            boolean atendido, Instant atendidoEn) {
        return jdbc.update(INSERTAR, UUID.randomUUID(), estanqueId, loteId, clase, nivel, variable,
                4.2, "La temperatura se salió de lo normal de este estanque.", Instant.now(),
                Instant.now(), atendido, atendidoEn);
    }

    // ---------- lo que sí se puede guardar ----------

    @Test
    void unHallazgoDeEstanqueSeGuarda() {
        assertThat(insertar(estanque(), null, "desviacion", "advertencia", "temp_c", false, null))
                .isEqualTo(1);
    }

    @Test
    void unHallazgoSinVariableSeGuarda() {
        // Los de combinacion y atipica miran varias variables a la vez: no hay una sola que nombrar
        assertThat(insertar(estanque(), null, "combinacion", "advertencia", null, false, null))
                .isEqualTo(1);
    }

    @Test
    void losTresNivelesSonValidos() {
        for (String nivel : new String[] {"informe", "advertencia", "critica"}) {
            assertThat(insertar(estanque(), null, "tendencia", nivel, "oxigeno_mg_l", false, null))
                    .as("nivel %s", nivel)
                    .isEqualTo(1);
        }
    }

    // ---------- ck_hallazgos_ia_de_uno_solo ----------

    @Test
    void unHallazgoSinEstanqueNiLoteNoSeGuarda() {
        // Un aviso que no dice de qué es, no es un aviso: nadie podría ir a mirar
        assertThatThrownBy(() -> insertar(null, null, "desviacion", "advertencia", "temp_c", false, null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_hallazgos_ia_de_uno_solo");
    }

    @Test
    void unHallazgoDeEstanqueYLoteALaVezNoSeGuarda() {
        UUID estanqueId = estanque();
        UUID loteId = UUID.randomUUID();
        jdbc.update("""
                insert into lotes (id, estanque_id, codigo, cantidad_inicial, estado,
                                   actualizado_en, servidor_en)
                values (?, ?, ?, 5000, 'activo', ?, ?)
                """, loteId, estanqueId, "L-" + loteId.toString().substring(0, 6),
                Instant.now(), Instant.now());

        // Los del agua son de un estanque y los del lote son de un lote. Los dos a la vez dejaría
        // la pregunta "¿de cuál es?" sin respuesta, y las consultas de la tesis contarían doble.
        assertThatThrownBy(() -> insertar(estanqueId, loteId, "mortalidad", "critica", null, false, null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_hallazgos_ia_de_uno_solo");
    }

    // ---------- ck_hallazgos_ia_nivel ----------

    @Test
    void unNivelInventadoNoSeGuarda() {
        // Si entrara, la pantalla no sabría de qué color pintarlo y el filtro por urgencia lo
        // dejaría fuera en silencio
        assertThatThrownBy(() -> insertar(estanque(), null, "desviacion", "urgentisimo", "temp_c", false, null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_hallazgos_ia_nivel");
    }

    // ---------- ck_hallazgos_ia_atendido_en ----------

    @Test
    void marcarAtendidoSinDecirCuandoNoSeGuarda() {
        // Es la misma regla que ya se exige en alertas: sin la hora no se puede medir cuánto tarda
        // el cuidador en responder, que es uno de los indicadores que la tesis quiere mostrar
        assertThatThrownBy(() -> insertar(estanque(), null, "desviacion", "critica", "temp_c", true, null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_hallazgos_ia_atendido_en");
    }

    @Test
    void atendidoConSuHoraSeGuarda() {
        assertThat(insertar(estanque(), null, "desviacion", "critica", "temp_c", true, Instant.now()))
                .isEqualTo(1);
    }

    @Test
    void sinAtenderNoHaceFaltaLaHora() {
        assertThat(insertar(estanque(), null, "desviacion", "critica", "temp_c", false, null))
                .isEqualTo(1);
    }

    // ---------- llaves foraneas ----------

    @Test
    void unHallazgoDeUnEstanqueQueNoExisteNoSeGuarda() {
        // Sin esto, borrar un estanque dejaría hallazgos huérfanos apuntando a nada
        assertThatThrownBy(() -> insertar(UUID.randomUUID(), null, "desviacion", "advertencia",
                "temp_c", false, null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------- lo que la pantalla necesita que sea rápido ----------

    @Test
    void existenLosIndicesQueUsaLaPantalla() {
        // Lo pendiente se consulta en cada apertura de la pantalla de alertas, y la comprobación de
        // repetidos corre una vez por hallazgo que devuelve el servicio de análisis
        assertThat(indices()).contains("IX_HALLAZGOS_IA_ATENDIDO", "IX_HALLAZGOS_IA_REPETIDOS",
                "IX_HALLAZGOS_IA_LOTE");
    }

    private java.util.List<String> indices() {
        return jdbc.queryForList(
                "select upper(index_name) from information_schema.indexes where upper(table_name) = 'HALLAZGOS_IA'",
                String.class);
    }
}
