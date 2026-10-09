package com.lacocha.backend.servicio;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lacocha.backend.dto.Dispositivos.DispositivoEditar;
import com.lacocha.backend.dto.Dispositivos.DispositivoSalida;
import com.lacocha.backend.dto.Dispositivos.SincronizacionSalida;
import com.lacocha.backend.modelo.Dispositivo;
import com.lacocha.backend.repositorio.DispositivoRepository;
import com.lacocha.backend.repositorio.SincronizacionRepository;

@Service
public class DispositivoService {

    private final DispositivoRepository dispositivos;
    private final SincronizacionRepository sincronizaciones;

    public DispositivoService(DispositivoRepository dispositivos, SincronizacionRepository sincronizaciones) {
        this.dispositivos = dispositivos;
        this.sincronizaciones = sincronizaciones;
    }

    /** Ordenados por el ultimo que sincronizo, que es como se revisa si alguno quedo colgado. */
    @Transactional(readOnly = true)
    public List<DispositivoSalida> listar() {
        return dispositivos.findAllByOrderByUltimoVistoEnDesc().stream().map(DispositivoSalida::de).toList();
    }

    /** Historial de envios, de un dispositivo o de todos. */
    @Transactional(readOnly = true)
    public List<SincronizacionSalida> sincronizaciones(String dispositivoId) {
        List<com.lacocha.backend.modelo.Sincronizacion> registros = dispositivoId == null
                ? sincronizaciones.findTop200ByOrderByServidorEnDesc()
                : sincronizaciones.findTop200ByDispositivoIdOrderByServidorEnDesc(dispositivoId);
        return registros.stream().map(SincronizacionSalida::de).toList();
    }

    /**
     * El dispositivo no se crea desde aca: lo crea el primer push. Esto sirve para ponerle
     * nombre al celular ("celular de Don Luis") y para darlo de baja cuando se pierde.
     *
     * No se borra nunca: los eventos guardados apuntan a el por llave foranea y son el
     * historial del cultivo. Dar de baja es poner activo en false, no eliminar la fila.
     */
    @Transactional
    public DispositivoSalida editar(String id, DispositivoEditar datos) {
        Dispositivo d = dispositivos.findById(id).orElseThrow(() -> CatalogoService.noExiste("El dispositivo"));
        if (datos.descripcion() != null) {
            d.setDescripcion(datos.descripcion());
        }
        if (datos.activo() != null) {
            d.setActivo(datos.activo());
        }
        dispositivos.flush();
        return DispositivoSalida.de(d);
    }
}
