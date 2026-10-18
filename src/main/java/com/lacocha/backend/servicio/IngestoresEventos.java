package com.lacocha.backend.servicio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.lacocha.backend.dto.Eventos.AlimentacionEntrada;
import com.lacocha.backend.dto.Eventos.BiometriaEntrada;
import com.lacocha.backend.dto.Eventos.ConteoEntrada;
import com.lacocha.backend.dto.Eventos.Entrada;
import com.lacocha.backend.dto.Eventos.LecturaAguaEntrada;
import com.lacocha.backend.dto.Eventos.MortalidadEntrada;
import com.lacocha.backend.modelo.Alerta;

/**
 * Las clases de evento que acepta el servidor, y qué dispara cada una una vez guardada.
 *
 * Es la única lista de las etiquetas del protocolo: el adaptador pregunta aquí en qué record tiene
 * que leer una trama cruda, y la tubería pregunta aquí qué hacer con ella después.
 */
@Component
public class IngestoresEventos {
    /** Lo que dispara una clase de evento una vez guardada. Devuelve cuántas alertas creó. */

    public interface Ingestor {
        int despuesDeGuardar(Entrada evento, AlmacenEventos almacen);
    }

    /**
     * Método fábrica: cada subclase responde por un "tipo" del protocolo y decide qué ingestor lo
     * atiende. Aceptar una clase nueva de evento es agregar una subclase y registrarla más abajo;

     * el adaptador, la tubería y los controladores no cambian.
     */
}

