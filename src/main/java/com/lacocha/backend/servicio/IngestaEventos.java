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
}

