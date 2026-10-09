package com.lacocha.backend.controlador;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lacocha.backend.dto.Dispositivos.DispositivoSalida;
import com.lacocha.backend.servicio.DispositivoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(name = "Dispositivos")
public class DispositivoController {

    private final DispositivoService servicio;

    public DispositivoController(DispositivoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/dispositivos")
    @Operation(summary = "Celulares y nodos que sincronizan, con la ultima vez que lo hicieron")
    public List<DispositivoSalida> listar() {
        return servicio.listar();
    }
}
