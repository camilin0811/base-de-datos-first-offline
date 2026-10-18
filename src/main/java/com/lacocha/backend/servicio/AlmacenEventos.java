package com.lacocha.backend.servicio;

import java.util.List;
import java.util.UUID;

import com.lacocha.backend.modelo.Alerta;
import com.lacocha.backend.modelo.Evento;

/**
 * Lado del implementador del patrón Bridge: todo lo que la ingesta de un evento necesita del
 * almacenamiento, y nada más.
 *
 * La tubería que está encima nunca ve un EntityManager. Eso es lo que permite probar las reglas de
 * qué se acepta, qué es duplicado y qué se rechaza sin una base de datos, y cambiar el motor de
 * persistencia sin tocar esas reglas.
 */
public interface AlmacenEventos {

    /** true cuando ya hay un evento con ese id: el celular reintentó tras perder la señal. */
    boolean existe(Evento evento);

    void guardar(Evento evento);

    /** El estanque (lecturas de agua) o el lote (los demás) al que apunta el evento. */
    boolean existePadre(boolean padreEsEstanque, UUID padreId);

    /** Guarda las alertas que levantó el sistema experto y devuelve cuántas quedaron. */
    int guardarAlertas(List<Alerta> alertas);
}
