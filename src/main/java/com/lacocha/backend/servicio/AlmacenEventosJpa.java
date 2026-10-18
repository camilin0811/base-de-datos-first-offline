package com.lacocha.backend.servicio;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.lacocha.backend.modelo.Alerta;
import com.lacocha.backend.modelo.Estanque;
import com.lacocha.backend.modelo.Evento;
import com.lacocha.backend.modelo.Lote;

import jakarta.persistence.EntityManager;

/**
 * El almacén que usa el servidor de verdad. Trabaja dentro de la transacción del push a propósito:
 * si el push se revierte, nada puede quedar como si hubiera entrado, y ninguna alerta puede quedar
 * apuntando a una lectura que no existe.
 */
@Component
public class AlmacenEventosJpa implements AlmacenEventos {

    private final EntityManager em;

    public AlmacenEventosJpa(EntityManager em) {
        this.em = em;
    }

    @Override
    public boolean existe(Evento evento) {
        // Cada clase de evento tiene su propia tabla, así que el id se busca solo en la que toca
        return em.find(evento.getClass(), evento.getId()) != null;
    }

    @Override
    public void guardar(Evento evento) {
        em.persist(evento);
    }

    @Override
    public boolean existePadre(boolean padreEsEstanque, UUID padreId) {
        // En una variable y no en línea: el ternario de dos clases de entidad rompe la inferencia
        Class<?> padre = padreEsEstanque ? Estanque.class : Lote.class;
        return em.find(padre, padreId) != null;
    }

    @Override
    public int guardarAlertas(List<Alerta> alertas) {
        alertas.forEach(em::persist);
        return alertas.size();
    }
}
