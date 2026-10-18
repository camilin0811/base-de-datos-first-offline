package com.lacocha.backend.servicio;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.lacocha.backend.dto.Eventos.Entrada;
import com.lacocha.backend.dto.Sync.Rechazo;
import com.lacocha.backend.modelo.Evento;
import com.lacocha.backend.servicio.AdaptadorJsonPush.EventoInvalido;
import com.lacocha.backend.servicio.DecoradoresAlmacenEventos.ConteoPorOrigen;
import com.lacocha.backend.servicio.DecoradoresAlmacenEventos.Rastro;

/**
 * Lado de la abstracción del patrón Bridge: las reglas de qué hace el servidor con los eventos de
 * un push, escritas una sola vez y sosteniendo un AlmacenEventos del que nunca sabe cuál es.
 *
 * Un evento se acepta cuando su padre existe y su id no se había visto. Uno repetido vuelve como
 * duplicado y no como error, porque el celular reenvía todo aquello de lo que no está seguro. Uno
 * malo se rechaza solo, para que una sola trama ilegible no bloquee el resto de la cola.
 */
@Component
public class IngestaEventos {
    private static final Logger log = LoggerFactory.getLogger(IngestaEventos.class);

    /** Lo que produjeron los eventos de un push. */

    public record Resultado(List<UUID> aceptados, List<UUID> duplicados, List<Rechazo> rechazados,
            int alertasGeneradas) {
    }

    private final AlmacenEventos almacen;

    private final AdaptadorJsonPush adaptador;

    private final IngestoresEventos ingestores;

    public IngestaEventos(AlmacenEventos almacen, AdaptadorJsonPush adaptador, IngestoresEventos ingestores) {
        this.almacen = almacen;
        this.adaptador = adaptador;
        this.ingestores = ingestores;
    }

    public Resultado ingerir(List<JsonNode> tramas, String dispositivoId, Instant servidorEn) {
        List<UUID> aceptados = new ArrayList<>();
        List<UUID> duplicados = new ArrayList<>();
        List<Rechazo> rechazados = new ArrayList<>();
        int alertasGeneradas = 0;

        // Los decoradores viven solo este push: una cuenta que sumara dos pushes no diría nada
        ConteoPorOrigen cuenta = new ConteoPorOrigen(Rastro.encendido() ? new Rastro(almacen) : almacen);
        // Un push suele traer muchos eventos del mismo lote; preguntar una vez alcanza
        Set<UUID> padresConocidos = new HashSet<>();

        for (JsonNode crudo : tramas) {
            Entrada evento;
            try {
                evento = adaptador.leerEvento(crudo);
            } catch (EventoInvalido ex) {
                rechazados.add(new Rechazo(AdaptadorJsonPush.idCrudo(crudo), ex.getMessage()));
                continue;
            }

            if (!padresConocidos.contains(evento.padreId())
                    && !cuenta.existePadre(evento.padreEsEstanque(), evento.padreId())) {
                String campo = evento.padreEsEstanque() ? "estanque_id" : "lote_id";
                rechazados.add(new Rechazo(evento.id().toString(), campo + ": no existe"));
                continue;
            }
            padresConocidos.add(evento.padreId());

            Evento entidad = sellar(evento, dispositivoId, servidorEn);
            if (cuenta.existe(entidad)) {
                duplicados.add(evento.id());
                continue;
            }
            cuenta.guardar(entidad);
            aceptados.add(evento.id());
            alertasGeneradas += ingestores.para(evento).despuesDeGuardar(evento, cuenta);
        }

        if (!aceptados.isEmpty()) {
            log.info("eventos de {}: {}", dispositivoId, cuenta.resumen());
        }
        return new Resultado(aceptados, duplicados, rechazados, alertasGeneradas);
    }

    /** El id lo pone el celular (es lo que hace idempotente el push); lo demás lo pone el servidor. */
}

