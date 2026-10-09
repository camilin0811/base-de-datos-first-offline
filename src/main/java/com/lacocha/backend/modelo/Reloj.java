package com.lacocha.backend.modelo;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Hora del servidor con la precision que de verdad soporta la base de datos.
 *
 * Instant.now() entrega nanosegundos, pero PostgreSQL guarda los timestamp con precision de
 * microsegundos y redondea. Sin truncar, la respuesta de la API traia una hora que la base
 * nunca guardo: la primera lectura devolvia ...825385800Z y la siguiente, ya leida de la
 * base, ...825386Z.
 *
 * Importa sobre todo para servidor_en, que es el cursor de /api/sync/pull: el celular manda
 * de vuelta ese valor y el servidor consulta servidor_en > desde. Un cursor con nanosegundos
 * de mas es un cursor mayor que lo que hay guardado, y podria saltarse una fila escrita en
 * el mismo microsegundo.
 */
public final class Reloj {

    private Reloj() {
    }

    public static Instant ahora() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
