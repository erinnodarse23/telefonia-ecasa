package cu.ecasa.api.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import cu.ecasa.api.dto.response.MonitoreoEstadoResponse;
import cu.ecasa.api.mapper.MonitoreoMapper;
import cu.ecasa.api.repository.MonitoreoRepository;

/**
 * Servicio del módulo de monitoreo.
 *
 * <p>Devuelve el estado de consumo por equipo en un mes concreto, con semáforo
 * verde/amarillo/rojo. Si el cliente no especifica {@code anio} o {@code mes},
 * se aplica la siguiente regla de default:
 * <ul>
 *   <li>Sin año → año actual</li>
 *   <li>Sin mes → si el año es el actual, mes actual; si no, diciembre (12)</li>
 * </ul>
 *
 * <p>El filtro por UEB lo aplica el controller con {@code scopedUeb()}.
 * Este servicio solo recibe el {@code uebId} ya resuelto (puede ser {@code null}
 * para ver todas las UEBs).
 *
 * @see cu.ecasa.api.repository.MonitoreoRepository
 * @see cu.ecasa.api.mapper.MonitoreoMapper
 */
@Service
public class MonitoreoService {

    private final MonitoreoRepository repository;
    private final MonitoreoMapper mapper;

    public MonitoreoService(MonitoreoRepository repository, MonitoreoMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * Devuelve el estado de monitoreo por equipo.
     *
     * @param anio  año (null → año actual)
     * @param mes   mes 1-12 (null → mes actual si el año es el actual, si no diciembre)
     * @param uebId ID de UEB (null → todas)
     * @return lista de DTOs ordenada por número de línea
     */
    public List<MonitoreoEstadoResponse> getEstado(Integer anio, Integer mes, Integer uebId) {
        LocalDate hoy = LocalDate.now();
        int anioFinal = anio != null ? anio : hoy.getYear();
        int mesFinal = mes != null
                ? mes
                : (anioFinal == hoy.getYear() ? hoy.getMonthValue() : 12);

        if (mesFinal < 1 || mesFinal > 12) {
            throw new IllegalArgumentException("El mes debe estar entre 1 y 12");
        }

        return mapper.toResponseList(repository.findEstado(anioFinal, mesFinal, uebId), anioFinal, mesFinal);
    }
}