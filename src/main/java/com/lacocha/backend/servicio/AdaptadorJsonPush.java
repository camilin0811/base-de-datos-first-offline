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

    public Entrada leerEvento(JsonNode crudo) throws EventoInvalido {
        if (crudo == null || !crudo.isObject()) {
            throw new EventoInvalido("el evento debe ser un objeto JSON");
        }
        Class<? extends Entrada> clase = ingestores.claseEntrada(crudo.path("tipo").asText(""));
        if (clase == null) {
            throw new EventoInvalido("tipo: debe ser uno de " + String.join(", ", ingestores.tipos()));
        }

        Entrada evento;
        try {
            evento = mapper.treeToValue(crudo, clase);
        } catch (JsonProcessingException ex) {
            throw new EventoInvalido(campoConError(ex) + ": formato no válido");
        }

        Set<ConstraintViolation<Entrada>> violaciones = validator.validate(evento);
        if (!violaciones.isEmpty()) {
            ConstraintViolation<Entrada> v = violaciones.stream()
                    .min(Comparator.comparing(x -> x.getPropertyPath().toString()))
                    .get();
            throw new EventoInvalido(aSnake(v.getPropertyPath().toString()) + ": " + v.getMessage());
        }

        String error = Fechas.errorFechaDispositivo(evento.registradoEn());
        if (error != null) {
            throw new EventoInvalido("registrado_en: " + error);
        }
        error = evento.errorExtra();
        if (error != null) {
            throw new EventoInvalido(error);
        }
        return evento;
    }

    /** El id de una trama que no se pudo ni leer, para que el celular igual sepa cuál fue. */

    public static String idCrudo(JsonNode crudo) {
        return crudo != null && crudo.hasNonNull("id") ? crudo.get("id").asText() : null;
    }

    /** Jackson ya reporta la ruta con los nombres del JSON. */

    private static String campoConError(JsonProcessingException ex) {
        if (ex instanceof MismatchedInputException m && !m.getPath().isEmpty()) {
            String campo = m.getPath().get(m.getPath().size() - 1).getFieldName();
            if (campo != null) {
                return campo;
            }
        }
        return "evento";
    }
}

