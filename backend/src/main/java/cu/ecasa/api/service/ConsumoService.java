package cu.ecasa.api.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cu.ecasa.api.repository.ConsumoRepository;
import cu.ecasa.api.repository.EquipoRepository;
import cu.ecasa.api.exceptions.ResourceNotFoundException;

/**
 * Servicio de gestión de consumos individuales.
 *
 * <p><b>Atomicidad:</b> {@link #update} es transaccional para garantizar que
 * el {@code UPDATE consumo} y la posible generación de alertas asociada
 * (que corre en su propia transacción con {@code REQUIRES_NEW}) no dejen
 * un estado inconsistente si falla la generación de alertas.
 *
 * @see ConsumoRepository
 * @see AlertGenerationService
 */
@Service
public class ConsumoService {

    private static final Logger log = LoggerFactory.getLogger(ConsumoService.class);

    private final ConsumoRepository repository;
    private final EquipoRepository equipoRepository;
    private final AlertGenerationService alertGenerationService;

    public ConsumoService(ConsumoRepository repository,
                          EquipoRepository equipoRepository,
                          AlertGenerationService alertGenerationService) {
        this.repository = repository;
        this.equipoRepository = equipoRepository;
        this.alertGenerationService = alertGenerationService;
    }

    /**
     * Lista los consumos de un equipo (todos los meses cargados).
     */
    public List<Map<String, Object>> findByEquipo(Integer equipoId) {
        if (equipoId == null || equipoId <= 0) {
            throw new IllegalArgumentException("ID de equipo inválido");
        }
        if (equipoRepository.findById(equipoId).isEmpty()) {
            throw new ResourceNotFoundException("Equipo no encontrado");
        }
        return repository.findByEquipo(equipoId);
    }

    /**
     * Actualiza manualmente un consumo.
     *
     * <p>Requiere al menos un valor no nulo. Los valores nulos se dejan
     * sin cambios (comportamiento COALESCE en el repositorio). Tras la
     * actualización se reevalúan las alertas (no bloqueante si falla).
     */
    @Transactional
    public Map<String, Object> update(Integer id, BigDecimal voz, BigDecimal datos, Integer sms) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID inválido");
        }
        validateValue(voz, "voz_consumido");
        validateValue(datos, "datos_consumido");
        if (sms != null && sms < 0) {
            throw new IllegalArgumentException("sms_consumido debe ser un número no negativo");
        }
        if (voz == null && datos == null && sms == null) {
            throw new IllegalArgumentException("Debe indicar al menos un valor");
        }
        if (repository.findById(id).isEmpty()) {
            throw new ResourceNotFoundException("Consumo no encontrado");
        }

        Map<String, Object> result = repository.update(id, voz, datos, sms);

        reevaluarAlertasTrasActualizacion();

        return result;
    }

    private void reevaluarAlertasTrasActualizacion() {
        try {
            alertGenerationService.evaluarTodasLasUebs();
        } catch (Exception ex) {
            log.warn("Reevaluación de alertas tras actualización falló: {}", ex.getMessage());
        }
    }

    private void validateValue(BigDecimal value, String name) {
        if (value != null && value.signum() < 0) {
            throw new IllegalArgumentException(name + " debe ser un número no negativo");
        }
    }
}