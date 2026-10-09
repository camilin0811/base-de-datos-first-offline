package com.lacocha.backend.servicio;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.lacocha.backend.dto.Catalogo.EstanqueCrear;
import com.lacocha.backend.dto.Catalogo.EstanqueEditar;
import com.lacocha.backend.dto.Catalogo.EstanqueSalida;
import com.lacocha.backend.dto.Catalogo.LoteCrear;
import com.lacocha.backend.dto.Catalogo.LoteEditar;
import com.lacocha.backend.dto.Catalogo.LoteSalida;
import com.lacocha.backend.modelo.Estanque;
import com.lacocha.backend.modelo.Lote;
import com.lacocha.backend.modelo.Reloj;
import com.lacocha.backend.repositorio.EstanqueRepository;
import com.lacocha.backend.repositorio.LoteRepository;

import jakarta.persistence.EntityManager;

@Service
public class CatalogoService {

    private final EntityManager em;
    private final EstanqueRepository estanques;
    private final LoteRepository lotes;

    public CatalogoService(EntityManager em, EstanqueRepository estanques, LoteRepository lotes) {
        this.em = em;
        this.estanques = estanques;
        this.lotes = lotes;
    }

    @Transactional(readOnly = true)
    public List<EstanqueSalida> listarEstanques() {
        return estanques.findAllByOrderByNombreAsc().stream().map(EstanqueSalida::de).toList();
    }

    @Transactional
    public EstanqueSalida crearEstanque(EstanqueCrear datos) {
        Estanque e = new Estanque();
        e.setId(datos.id() != null ? datos.id() : UUID.randomUUID());
        e.setNombre(datos.nombre());
        if (datos.tipo() != null) {
            e.setTipo(datos.tipo());
        }
        e.setVolumenM3(datos.volumenM3());
        e.setActualizadoEn(Reloj.ahora());
        em.persist(e);
        return EstanqueSalida.de(e);
    }

    @Transactional
    public EstanqueSalida editarEstanque(UUID id, EstanqueEditar datos) {
        Estanque e = estanques.findById(id).orElseThrow(() -> noExiste("El estanque"));
        if (datos.nombre() != null) e.setNombre(datos.nombre());
        if (datos.tipo() != null) e.setTipo(datos.tipo());
        if (datos.volumenM3() != null) e.setVolumenM3(datos.volumenM3());
        if (datos.activo() != null) e.setActivo(datos.activo());
        e.setActualizadoEn(Reloj.ahora());
        estanques.flush();
        return EstanqueSalida.de(e);
    }

    @Transactional(readOnly = true)
    public List<LoteSalida> listarLotes(UUID estanqueId, String estado) {
        return lotes.findAll().stream()
                .filter(l -> estanqueId == null || l.getEstanqueId().equals(estanqueId))
                .filter(l -> estado == null || l.getEstado().equals(estado))
                .sorted((a, b) -> a.getCodigo().compareTo(b.getCodigo()))
                .map(LoteSalida::de)
                .toList();
    }

    @Transactional
    public LoteSalida crearLote(LoteCrear datos) {
        if (!estanques.existsById(datos.estanqueId())) {
            throw noExiste("El estanque");
        }
        Lote l = new Lote();
        l.setId(datos.id() != null ? datos.id() : UUID.randomUUID());
        l.setEstanqueId(datos.estanqueId());
        l.setCodigo(datos.codigo());
        l.setFechaSiembra(datos.fechaSiembra());
        l.setCantidadInicial(datos.cantidadInicial());
        l.setPesoInicialG(datos.pesoInicialG());
        if (datos.estado() != null) {
            l.setEstado(datos.estado());
        }
        l.setFechaCierre(datos.fechaCierre());
        ajustarCierre(l);
        l.setActualizadoEn(Reloj.ahora());
        em.persist(l);
        return LoteSalida.de(l);
    }

    @Transactional
    public LoteSalida editarLote(UUID id, LoteEditar datos) {
        Lote l = lotes.findById(id).orElseThrow(() -> noExiste("El lote"));
        if (datos.codigo() != null) l.setCodigo(datos.codigo());
        if (datos.fechaSiembra() != null) l.setFechaSiembra(datos.fechaSiembra());
        if (datos.cantidadInicial() != null) l.setCantidadInicial(datos.cantidadInicial());
        if (datos.pesoInicialG() != null) l.setPesoInicialG(datos.pesoInicialG());
        if (datos.estado() != null) l.setEstado(datos.estado());
        if (datos.fechaCierre() != null) l.setFechaCierre(datos.fechaCierre());
        ajustarCierre(l);
        l.setActualizadoEn(Reloj.ahora());
        lotes.flush();
        return LoteSalida.de(l);
    }

    /**
     * La base exige que un lote cerrado tenga fecha de cierre y que uno activo no la tenga.
     * Se acomoda aca en vez de devolverle un 409 al cliente: cerrar sin decir la fecha quiere
     * decir "se cerro hoy", y reabrir un lote quiere decir que la fecha de cierre ya no aplica.
     */
    static void ajustarCierre(Lote l) {
        if ("cerrado".equals(l.getEstado())) {
            if (l.getFechaCierre() == null) {
                l.setFechaCierre(LocalDate.now());
            }
        } else {
            l.setFechaCierre(null);
        }
    }

    static ResponseStatusException noExiste(String que) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, que + " no existe");
    }
}
