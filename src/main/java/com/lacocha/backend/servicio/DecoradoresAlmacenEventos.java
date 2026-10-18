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
}

