package com.lacocha.backend.controlador;

import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

/** Chequeo de Render: no pide clave y confirma que la base de datos responde. */
@RestController
@Tag(name = "Sistema")
public class SaludController {

    private final JdbcTemplate jdbc;

    public SaludController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/salud")
    public Map<String, String> salud() {
        jdbc.queryForObject("select 1", Integer.class);
        return Map.of("estado", "ok");
    }
}
