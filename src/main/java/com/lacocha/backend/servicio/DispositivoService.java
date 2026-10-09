package com.lacocha.backend.servicio;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lacocha.backend.dto.Dispositivos.DispositivoSalida;
import com.lacocha.backend.repositorio.DispositivoRepository;

@Service
public class DispositivoService {

    private final DispositivoRepository dispositivos;

    public DispositivoService(DispositivoRepository dispositivos) {
        this.dispositivos = dispositivos;
    }

    /** Ordenados por el ultimo que sincronizo, que es como se revisa si alguno quedo colgado. */
    @Transactional(readOnly = true)
    public List<DispositivoSalida> listar() {
        return dispositivos.findAllByOrderByUltimoVistoEnDesc().stream().map(DispositivoSalida::de).toList();
    }
}
