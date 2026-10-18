package com.lacocha.backend.controlador;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.lacocha.backend.dto.Catalogo.EstanqueCrear;
import com.lacocha.backend.dto.Catalogo.EstanqueEditar;
import com.lacocha.backend.dto.Catalogo.EstanqueSalida;
import com.lacocha.backend.dto.Catalogo.LoteCrear;
import com.lacocha.backend.dto.Catalogo.LoteEditar;
import com.lacocha.backend.dto.Catalogo.LoteSalida;
import com.lacocha.backend.servicio.PisciculturaFacade;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
@Tag(name = "Catálogo")
public class CatalogoController {

    private final PisciculturaFacade facade;

    public CatalogoController(PisciculturaFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/estanques")
    public List<EstanqueSalida> listarEstanques() {
        return facade.listarEstanques();
    }

    @PostMapping("/estanques")
    @ResponseStatus(HttpStatus.CREATED)
    public EstanqueSalida crearEstanque(@Valid @RequestBody EstanqueCrear datos) {
        return facade.crearEstanque(datos);
    }

    @PatchMapping("/estanques/{id}")
    public EstanqueSalida editarEstanque(@PathVariable UUID id, @Valid @RequestBody EstanqueEditar datos) {
        return facade.editarEstanque(id, datos);
    }

    @GetMapping("/lotes")
    public List<LoteSalida> listarLotes(
            @RequestParam(name = "estanque_id", required = false) UUID estanqueId,
            @RequestParam(required = false) String estado) {
        return facade.listarLotes(estanqueId, estado);
    }

    @PostMapping("/lotes")
    @ResponseStatus(HttpStatus.CREATED)
    public LoteSalida crearLote(@Valid @RequestBody LoteCrear datos) {
        return facade.crearLote(datos);
    }

    @PatchMapping("/lotes/{id}")
    public LoteSalida editarLote(@PathVariable UUID id, @Valid @RequestBody LoteEditar datos) {
        return facade.editarLote(id, datos);
    }
}
