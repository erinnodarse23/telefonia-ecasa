package cu.ecasa.api.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cu.ecasa.api.repository.ConsumoRepository;
import cu.ecasa.api.repository.EquipoRepository;
import cu.ecasa.api.exceptions.ResourceNotFoundException;

/**
 * Servicio de gestión de consumos individuales.
 *
 * <p><b>Atomicidad:</b> {@link #update} y {@link #create} son transaccionales
 * para garantizar que la escritura y la posible generación de alertas
 * asociada (que corre en su propia transacción con {@code REQUIRES_NEW})
 * no dejen un estado inconsistente.
 *
 * @see ConsumoRepository
 * @see AlertGenerationService
 */
@Service
public class ConsumoService {

    private static final Logger log = LoggerFactory.getLogger(ConsumoService.class);

    private static final int ANIO_MIN = 2000;
    private static final int ANIO_MAX = 2100;

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
     * Crea un consumo manual.
     *
     * <p>Validaciones estrictas:
     * <ul>
     *   <li>{@code equipoId}: obligatorio, &gt; 0, debe existir en la BD</li>
     *   <li>{@code anio}: obligatorio, entre 2000 y 2100</li>
     *   <li>{@code mes}: obligatorio, entre 1 y 12</li>
     *   <li>{@code voz}, {@code datos}: opcionales, &ge; 0</li>
     *   <li>{@code sms}: opcional, entero &ge; 0</li>
     * </ul>
     *
     * <p>Si ya existe un consumo para {@code (equipoId, anio, mes)}, lanza
     * {@link DataIntegrityViolationException} → el {@code ApiExceptionHandler}
     * lo mapea a {@code 409 Conflict}.
     *
     * @return la fila creada
     */
    @Transactional
    public Map<String, Object> create(Integer equipoId, Integer anio, Integer mes,
                                      BigDecimal voz, BigDecimal datos, Integer sms) {
        // --- 1. Equipo ---
        if (equipoId == null || equipoId <= 0) {
            throw new IllegalArgumentException("ID de equipo inválido");
        }
        if (equipoRepository.findById(equipoId).isEmpty()) {
            throw new ResourceNotFoundException("Equipo no encontrado");
        }

        // --- 2. Año ---
        if (anio == null) {
            throw new IllegalArgumentException("El año es obligatorio");
        }
        if (anio < ANIO_MIN || anio > ANIO_MAX) {
            throw new IllegalArgumentException(
                    "El año debe estar entre " + ANIO_MIN + " y " + ANIO_MAX);
        }

        // --- 3. Mes ---
        if (mes == null) {
            throw new IllegalArgumentException("El mes es obligatorio");
        }
        if (mes < 1 || mes > 12) {
            throw new IllegalArgumentException("El mes debe estar entre 1 y 12");
        }

        // --- 4. Valores ---
        validateValue(voz, "voz_consumido");
        validateValue(datos, "datos_consumido");
        if (sms != null && sms < 0) {
            throw new IllegalArgumentException("sms_consumido debe ser un número no negativo");
        }

        // --- 5. Duplicado ---
        if (repository.existsByEquipoAndPeriodo(equipoId, anio, mes)) {
            throw new DataIntegrityViolationException(
                    "Ya existe un consumo para el equipo " + equipoId
                            + " en el período " + anio + "-" + mes
                            + ". Use el endpoint de actualización para modificarlo.");
        }

        // --- 6. Insertar ---
        Map<String, Object> result = repository.insertOrUpdate(equipoId, anio, mes, voz, datos, sms);

        // --- 7. Reevaluar alertas (no bloqueante) ---
        reevaluarAlertasTrasActualizacion();

        return result;
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