package com.lacocha.backend.servicio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lacocha.backend.dto.Parametros.FactorSalida;
import com.lacocha.backend.dto.Parametros.ParametrosSalida;
import com.lacocha.backend.dto.Parametros.RangoSalida;
import com.lacocha.backend.dto.Parametros.TasaSalida;
import com.lacocha.backend.repositorio.FactorTemperaturaRepository;
import com.lacocha.backend.repositorio.ParametroRangoRepository;
import com.lacocha.backend.repositorio.TasaAlimentacionRepository;

@Service
public class ParametroService {

    private final ParametroRangoRepository rangos;
    private final TasaAlimentacionRepository tasas;
    private final FactorTemperaturaRepository factores;

    public ParametroService(ParametroRangoRepository rangos, TasaAlimentacionRepository tasas,
            FactorTemperaturaRepository factores) {
        this.rangos = rangos;
        this.tasas = tasas;
        this.factores = factores;
    }

    /**
     * Deja a la vista con que numeros esta decidiendo el sistema experto y de donde salio
     * cada uno. Mientras la fuente diga que estan sin citar, el panel puede advertirlo.
     */
    @Transactional(readOnly = true)
    public ParametrosSalida parametros() {
        return new ParametrosSalida(
                rangos.findAllByOrderByVariableAsc().stream().map(RangoSalida::de).toList(),
                tasas.findAllByOrderByOrdenAsc().stream().map(TasaSalida::de).toList(),
                factores.findAllByOrderByOrdenAsc().stream().map(FactorSalida::de).toList());
    }
}
