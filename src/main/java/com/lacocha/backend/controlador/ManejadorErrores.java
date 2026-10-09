package com.lacocha.backend.controlador;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

/** Todos los errores salen como {"detalle": "..."} para que la app los muestre igual. */
@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> estado(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("detalle", String.valueOf(ex.getReason())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validacion(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("detalle", detalle));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> jsonInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(Map.of("detalle", "El cuerpo no es un JSON válido o tiene campos con formato incorrecto"));
    }

    /**
     * Choque con una restriccion de la base (nombre de estanque repetido, por ejemplo).
     * Sale 409 y no 500 porque no es una falla del servidor: es un dato que el usuario
     * puede corregir. El mensaje se arma con el nombre de la restriccion para no
     * devolverle el texto crudo de PostgreSQL a la app.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> integridad(DataIntegrityViolationException ex) {
        String causa = String.valueOf(ex.getMostSpecificCause().getMessage()).toLowerCase();
        String detalle;
        if (causa.contains("uq_estanques_nombre")) {
            detalle = "Ya existe un estanque con ese nombre";
        } else if (causa.contains("uq_lotes_estanque_codigo")) {
            detalle = "Ya existe un lote con ese código en el estanque";
        } else {
            detalle = "El dato no cumple una restricción de la base de datos";
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("detalle", detalle));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> parametro(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(Map.of("detalle", ex.getName() + ": formato no válido"));
    }
}
