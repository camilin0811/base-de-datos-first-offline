package com.lacocha.backend.servicio;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;

final class Fechas {

    private Fechas() {
    }

    /** Un celular con el reloj adelantado ensuciaría el historial y ganaría siempre en los cambios del catálogo. */
    static String errorFechaDispositivo(OffsetDateTime fecha) {
        if (fecha != null && fecha.toInstant().isAfter(Instant.now().plus(Duration.ofDays(1)))) {
            return "la fecha está en el futuro: revisa el reloj del celular";
        }
        return null;
    }
}
