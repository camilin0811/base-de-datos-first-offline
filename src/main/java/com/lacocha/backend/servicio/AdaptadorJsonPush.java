package com.lacocha.backend.servicio;

import java.util.Comparator;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.lacocha.backend.dto.Eventos.Entrada;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

/**
 * Adaptador entre lo que el dispositivo pone en el cable y los objetos de dominio del sistema.
 *
 * El celular y el nodo ESP32 suben JSON sin tipo: una trama del contador de alevinos llega como un
 * objeto cuyo campo "tipo" es la única pista de qué es. Este es el único lugar que la convierte en
 * un record ya validado, y el que nombra el campo que falló como lo nombra el JSON y no como lo
 * nombra Java, porque ese mensaje lo lee el cuidador.
 *
 * Los estanques y los lotes no pasan por aquí: esos llegan ya tipados en la petición.
 */
@Component
public class AdaptadorJsonPush {
    /** Un evento que no se puede guardar. Se rechaza solo; el resto del push sigue. */

    public static class EventoInvalido extends Exception {
        public EventoInvalido(String mensaje) {
            super(mensaje);
        }
    }

    private final ObjectMapper mapper;

    private final Validator validator;

    private final IngestoresEventos ingestores;

    public AdaptadorJsonPush(ObjectMapper mapper, Validator validator, IngestoresEventos ingestores) {
        this.mapper = mapper;
        this.validator = validator;
        this.ingestores = ingestores;
    }

    /** Trama cruda -> evento tipado, con sus anotaciones y las reglas que no caben en una. */
}

