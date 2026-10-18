package com.lacocha.backend.servicio;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.lacocha.backend.dto.Catalogo.EstanqueSalida;
import com.lacocha.backend.dto.Catalogo.EstanqueSync;
import com.lacocha.backend.dto.Catalogo.LoteSalida;
import com.lacocha.backend.dto.Catalogo.LoteSync;
import com.lacocha.backend.dto.Sync.AlertaSalida;
import com.lacocha.backend.dto.Sync.PullRespuesta;
import com.lacocha.backend.dto.Sync.PushPeticion;
import com.lacocha.backend.dto.Sync.PushRespuesta;
import com.lacocha.backend.dto.Sync.Rechazo;
import com.lacocha.backend.modelo.Dispositivo;
import com.lacocha.backend.modelo.Estanque;
import com.lacocha.backend.modelo.Lote;
import com.lacocha.backend.modelo.Reloj;
import com.lacocha.backend.modelo.Sincronizacion;
import com.lacocha.backend.repositorio.AlertaRepository;
import com.lacocha.backend.repositorio.DispositivoRepository;
import com.lacocha.backend.repositorio.EstanqueRepository;
import com.lacocha.backend.repositorio.LoteRepository;

import jakarta.persistence.EntityManager;

/**
 * Sincronización offline-first con la app del cuidador.
 *
 * push: el celular sube lo que tiene pendiente en su cola. Es idempotente: si la señal se cae a mitad
 *       del envío y el celular reintenta, los eventos repetidos salen en "duplicados" y no se guardan dos veces.
 *       El catálogo se fusiona aquí; los eventos se los lleva IngestaEventos.
 * pull: el celular baja los cambios del catálogo hechos desde el panel o desde otro celular.
 */
@Service
public class SyncService {

    private final EntityManager em;
    private final EstanqueRepository estanques;
    private final LoteRepository lotes;
    private final AlertaRepository alertas;
    private final DispositivoRepository dispositivos;
    private final IngestaEventos eventos;

    public SyncService(EntityManager em, EstanqueRepository estanques, LoteRepository lotes,
            AlertaRepository alertas, DispositivoRepository dispositivos, IngestaEventos eventos) {
        this.em = em;
        this.estanques = estanques;
        this.lotes = lotes;
        this.alertas = alertas;
        this.dispositivos = dispositivos;
        this.eventos = eventos;
    }

    /** Resultado que se va llenando durante el push. */
    private static final class Resultado {
        final List<UUID> aceptados = new ArrayList<>();
        final List<UUID> duplicados = new ArrayList<>();
        final List<UUID> obsoletos = new ArrayList<>();
        final List<Rechazo> rechazados = new ArrayList<>();
        int alertasGeneradas = 0;
    }

    @Transactional
    public PushRespuesta push(PushPeticion peticion) {
        Instant servidorEn = Reloj.ahora();
        Resultado r = new Resultado();
        registrarDispositivo(peticion.dispositivoId(), servidorEn);

        fusionarEstanques(lista(peticion.estanques()), r);
        em.flush();
        fusionarLotes(lista(peticion.lotes()), r);
        // Se vacía antes de los eventos: alguno puede apuntar a un lote que llegó en este mismo push
        em.flush();

        IngestaEventos.Resultado ingesta = eventos.ingerir(lista(peticion.eventos()),
                peticion.dispositivoId(), servidorEn);
        r.aceptados.addAll(ingesta.aceptados());
        r.duplicados.addAll(ingesta.duplicados());
        r.rechazados.addAll(ingesta.rechazados());
        r.alertasGeneradas += ingesta.alertasGeneradas();

        anotarEnLaBitacora(peticion.dispositivoId(), r, servidorEn);
        return new PushRespuesta(r.aceptados, r.duplicados, r.obsoletos, r.rechazados, r.alertasGeneradas, servidorEn);
    }

    private void fusionarEstanques(List<EstanqueSync> entrantes, Resultado r) {
        for (EstanqueSync e : entrantes) {
            String error = Fechas.errorFechaDispositivo(e.actualizadoEn());
            if (error != null) {
                r.rechazados.add(new Rechazo(e.id().toString(), "actualizado_en: " + error));
                continue;
            }
            Estanque actual = estanques.findById(e.id()).orElse(null);
            boolean nuevo = actual == null;
            if (nuevo) {
                actual = new Estanque();
                actual.setId(e.id());
            } else if (!e.actualizadoEn().toInstant().isAfter(actual.getActualizadoEn())) {
                r.obsoletos.add(e.id());
                continue;
            }
            if (nombreOcupado(e.id(), e.nombre())) {
                r.rechazados.add(new Rechazo(e.id().toString(), "nombre: ya existe otro estanque con ese nombre"));
                continue;
            }
            actual.setNombre(e.nombre());
            actual.setTipo(e.tipo() != null ? e.tipo() : "estanque");
            actual.setVolumenM3(e.volumenM3());
            actual.setActivo(e.activo() == null || e.activo());
            actual.setActualizadoEn(e.actualizadoEn().toInstant());
            if (nuevo) {
                em.persist(actual);
            }
            r.aceptados.add(e.id());
        }
    }

    private void fusionarLotes(List<LoteSync> entrantes, Resultado r) {
        for (LoteSync l : entrantes) {
            String error = Fechas.errorFechaDispositivo(l.actualizadoEn());
            if (error != null) {
                r.rechazados.add(new Rechazo(l.id().toString(), "actualizado_en: " + error));
                continue;
            }
            if (!estanques.existsById(l.estanqueId())) {
                r.rechazados.add(new Rechazo(l.id().toString(), "estanque_id: el estanque no existe"));
                continue;
            }
            Lote actual = lotes.findById(l.id()).orElse(null);
            boolean nuevo = actual == null;
            if (nuevo) {
                actual = new Lote();
                actual.setId(l.id());
            } else if (!l.actualizadoEn().toInstant().isAfter(actual.getActualizadoEn())) {
                r.obsoletos.add(l.id());
                continue;
            }
            if (codigoOcupado(l.id(), l.estanqueId(), l.codigo())) {
                r.rechazados.add(new Rechazo(l.id().toString(), "codigo: ya existe otro lote con ese código en el estanque"));
                continue;
            }
            actual.setEstanqueId(l.estanqueId());
            actual.setCodigo(l.codigo());
            actual.setFechaSiembra(l.fechaSiembra());
            actual.setCantidadInicial(l.cantidadInicial());
            actual.setPesoInicialG(l.pesoInicialG());
            actual.setEstado(l.estado() != null ? l.estado() : "activo");
            actual.setFechaCierre(l.fechaCierre());
            CatalogoService.ajustarCierre(actual);
            actual.setActualizadoEn(l.actualizadoEn().toInstant());
            if (nuevo) {
                em.persist(actual);
            }
            r.aceptados.add(l.id());
        }
    }

    /** desde es el servidor_en que devolvió el pull anterior. Sin él, baja todo. */
    @Transactional(readOnly = true)
    public PullRespuesta pull(Instant desde) {
        Instant servidorEn = Reloj.ahora();
        List<Estanque> listaEstanques = desde == null ? estanques.findAll() : estanques.findByServidorEnAfter(desde);
        List<Lote> listaLotes = desde == null ? lotes.findAll() : lotes.findByServidorEnAfter(desde);
        return new PullRespuesta(
                listaEstanques.stream().map(EstanqueSalida::de).toList(),
                listaLotes.stream().map(LoteSalida::de).toList(),
                alertas.findTop100ByAtendidaFalseOrderByMedidoEnDesc().stream().map(AlertaSalida::de).toList(),
                servidorEn);
    }

    /**
     * Guarda lo que paso en este push. Es la misma cuenta que se le devuelve al celular, que
     * hasta ahora se iba con la respuesta HTTP sin dejar rastro en el servidor.
     *
     * Va dentro de la transaccion del push a proposito: si el push se cae y se revierte, no
     * tiene sentido que quede anotado como si hubiera entrado.
     */
    private void anotarEnLaBitacora(String dispositivoId, Resultado r, Instant servidorEn) {
        Sincronizacion registro = new Sincronizacion();
        registro.setDispositivoId(dispositivoId);
        registro.setAceptados(r.aceptados.size());
        registro.setDuplicados(r.duplicados.size());
        registro.setObsoletos(r.obsoletos.size());
        registro.setRechazados(r.rechazados.size());
        registro.setAlertasGeneradas(r.alertasGeneradas);
        registro.setServidorEn(servidorEn);
        em.persist(registro);
    }

    /**
     * Deja constancia del celular que esta sincronizando y actualiza su ultimo_visto_en.
     *
     * Va antes de guardar los eventos porque dispositivo_id es llave foranea: si el
     * dispositivo no existe todavia, el INSERT del evento no pasaria. El flush asegura que
     * la fila este en la base antes de que empiecen a entrar los eventos.
     */
    private void registrarDispositivo(String id, Instant ahora) {
        Dispositivo dispositivo = dispositivos.findById(id).orElse(null);
        if (dispositivo == null) {
            dispositivo = new Dispositivo();
            dispositivo.setId(id);
            dispositivo.setPrimerVistoEn(ahora);
            dispositivo.setUltimoVistoEn(ahora);
            // Los campos van completos antes del persist: Hibernate copia el estado de la
            // entidad en ese momento y no vuelve a leerlo al hacer el flush.
            em.persist(dispositivo);
        } else if (!dispositivo.isActivo()) {
            // Un celular dado de baja (perdido, robado o que salio de la finca) no puede
            // seguir escribiendo. Se corta el push completo y no se registra el intento:
            // el dato de un equipo que ya no esta autorizado no es confiable.
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "El dispositivo " + id + " esta dado de baja");
        } else {
            dispositivo.setUltimoVistoEn(ahora);
        }
        em.flush();
    }

    /**
     * El nombre del estanque y el codigo del lote son unicos en la base. Si el choque llegara
     * hasta el INSERT, la transaccion entera del push se caeria y el celular perderia toda la
     * cola, no solo el elemento malo. Por eso se detecta antes y se rechaza solo ese.
     *
     * La consulta fuerza el flush de lo que lleva la transaccion, asi que tambien detecta dos
     * estanques con el mismo nombre dentro del mismo envio.
     */
    private boolean nombreOcupado(UUID id, String nombre) {
        return estanques.findByNombre(nombre).filter(otro -> !otro.getId().equals(id)).isPresent();
    }

    private boolean codigoOcupado(UUID id, UUID estanqueId, String codigo) {
        return lotes.findByEstanqueIdAndCodigo(estanqueId, codigo)
                .filter(otro -> !otro.getId().equals(id))
                .isPresent();
    }

    private static <T> List<T> lista(List<T> l) {
        return l != null ? l : List.of();
    }
}
