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
}

