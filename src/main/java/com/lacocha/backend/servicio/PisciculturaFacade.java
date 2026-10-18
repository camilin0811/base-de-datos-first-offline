package com.lacocha.backend.servicio;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.lacocha.backend.dto.Catalogo.EstanqueCrear;
import com.lacocha.backend.dto.Catalogo.EstanqueEditar;
import com.lacocha.backend.dto.Catalogo.EstanqueSalida;
import com.lacocha.backend.dto.Catalogo.LoteCrear;
import com.lacocha.backend.dto.Catalogo.LoteEditar;
import com.lacocha.backend.dto.Catalogo.LoteSalida;
import com.lacocha.backend.dto.Consultas.LecturaAguaSalida;
import com.lacocha.backend.dto.Consultas.ResumenLote;
import com.lacocha.backend.dto.Dispositivos.DispositivoEditar;
import com.lacocha.backend.dto.Dispositivos.DispositivoSalida;
import com.lacocha.backend.dto.Dispositivos.SincronizacionSalida;
import com.lacocha.backend.dto.Parametros.ParametrosSalida;
import com.lacocha.backend.dto.Sync.AlertaSalida;
import com.lacocha.backend.dto.Sync.PullRespuesta;
import com.lacocha.backend.dto.Sync.PushPeticion;
import com.lacocha.backend.dto.Sync.PushRespuesta;

/**
 * La única puerta de entrada al sistema para la capa HTTP.
 *
 * Detrás viven los cuatro subsistemas que los controladores alcanzaban uno por uno: el catálogo de
 * estanques y lotes, la sincronización con los celulares, la lectura de los eventos que subieron
 * los dispositivos, y los parámetros del sistema experto. Un controlador solo tiene que saber su
 * ruta, su JSON y a qué caso de uso llamar; en qué servicio vive ese caso de uso, y si mañana se
 * mueve de sitio, no es asunto suyo.
 *
 * La regla se revisa con un grep: un controlador inyecta esta fachada y ningún servicio.
 */
@Service
public class PisciculturaFacade {
    private final CatalogoService catalogo;

    private final ConsultaService consultas;

    private final SyncService sync;

    private final DispositivoService dispositivos;

    private final ParametroService parametros;

    public PisciculturaFacade(CatalogoService catalogo, ConsultaService consultas, SyncService sync,
            DispositivoService dispositivos, ParametroService parametros) {
        this.catalogo = catalogo;
        this.consultas = consultas;
        this.sync = sync;
        this.dispositivos = dispositivos;
        this.parametros = parametros;
    }

    // ---------- Catálogo: estanques y lotes, editables desde el panel y desde cualquier celular ----------

    public List<EstanqueSalida> listarEstanques() {
        return catalogo.listarEstanques();
    }

    public EstanqueSalida crearEstanque(EstanqueCrear datos) {
        return catalogo.crearEstanque(datos);
    }

    public EstanqueSalida editarEstanque(UUID id, EstanqueEditar datos) {
        return catalogo.editarEstanque(id, datos);
    }

    public List<LoteSalida> listarLotes(UUID estanqueId, String estado) {
        return catalogo.listarLotes(estanqueId, estado);
    }

    public LoteSalida crearLote(LoteCrear datos) {
        return catalogo.crearLote(datos);
    }

    public LoteSalida editarLote(UUID id, LoteEditar datos) {
        return catalogo.editarLote(id, datos);
    }
}

