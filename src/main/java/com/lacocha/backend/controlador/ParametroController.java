package com.lacocha.backend.controlador;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lacocha.backend.dto.Parametros.ParametrosSalida;
import com.lacocha.backend.servicio.ParametroService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(name = "Parámetros")
public class ParametroController {

    private final ParametroService servicio;

    public ParametroController(ParametroService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/parametros")
    @Operation(summary = "Umbrales de calidad de agua y curva de alimentación, con su fuente")
    public ParametrosSalida parametros() {
        return servicio.parametros();
    }
}
