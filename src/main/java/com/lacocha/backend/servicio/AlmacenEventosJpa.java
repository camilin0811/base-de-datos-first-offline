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
}

