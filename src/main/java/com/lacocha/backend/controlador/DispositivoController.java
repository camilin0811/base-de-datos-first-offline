package com.lacocha.backend.controlador;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lacocha.backend.dto.Dispositivos.DispositivoEditar;
import com.lacocha.backend.dto.Dispositivos.DispositivoSalida;
import com.lacocha.backend.dto.Dispositivos.SincronizacionSalida;
import com.lacocha.backend.servicio.DispositivoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

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

    @GetMapping("/sincronizaciones")
    @Operation(summary = "Historial de envios: aceptados, duplicados y rechazados de cada push")
    public List<SincronizacionSalida> sincronizaciones(
            @RequestParam(name = "dispositivo_id", required = false) String dispositivoId) {
        return servicio.sincronizaciones(dispositivoId);
    }

    @PatchMapping("/dispositivos/{id}")
    @Operation(summary = "Ponerle nombre a un dispositivo o darlo de baja")
    public DispositivoSalida editar(@PathVariable String id, @Valid @RequestBody DispositivoEditar datos) {
        return servicio.editar(id, datos);
    }
}
