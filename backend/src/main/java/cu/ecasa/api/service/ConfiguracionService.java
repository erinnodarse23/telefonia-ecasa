package cu.ecasa.api.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cu.ecasa.api.repository.ConfiguracionRepository;
import cu.ecasa.api.repository.UebRepository;
import cu.ecasa.api.exceptions.ResourceNotFoundException;

/**
 * Servicio de configuración del sistema (umbrales, planes anuales, parámetros).
 *
 * <p><b>Atomicidad:</b> las operaciones de escritura son transaccionales para
 * garantizar que las múltiples mutaciones (ej: {@link #saveThresholds} hace
 * N UPSERTs) se ejecuten como una unidad. Si una falla, se revierten todas.
 *
 * @see ConfiguracionRepository
 */
@Service
public class ConfiguracionService {

    private final ConfiguracionRepository repository;
    private final UebRepository uebRepository;
    private final ObjectMapper objectMapper;

    public ConfiguracionService(ConfiguracionRepository repository,
                                UebRepository uebRepository,
                                ObjectMapper objectMapper) {
        this.repository = repository;
        this.uebRepository = uebRepository;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> annualPlans(Integer year) {
        return repository.annualPlans(year);
    }

    public Map<String, Object> annualPlan(Integer uebId, Integer year) {
        Map<String, Object> result = repository.annualPlan(uebId, year);
        if (result == null) throw new ResourceNotFoundException("Plan no encontrado para la UEB y año especificados");
        return result;
    }

    @Transactional
    public Map<String, Object> saveAnnualPlan(Integer uebId, Integer year, BigDecimal amount) {
        if (uebRepository.findById(uebId).isEmpty()) throw new ResourceNotFoundException("UEB no encontrada");
        if (amount == null || amount.signum() < 0) throw new IllegalArgumentException("El monto debe ser un número no negativo");
        return repository.saveAnnualPlan(uebId, year, amount);
    }

    public Object jsonConfig(String type, Integer year) {
        Map<String, Object> row = repository.jsonConfig(type, year);
        if (row == null) return null;
        Object raw = row.get("valor_json");
        try {
            return objectMapper.readTree(String.valueOf(raw));
        } catch (Exception ignored) {
            return raw;
        }
    }

    @Transactional
    public void saveJsonConfig(String type, Integer year, JsonNode value) {
        if (value == null || value.isNull()) throw new IllegalArgumentException("El valor es obligatorio");
        try {
            repository.saveJsonConfig(type, year, objectMapper.writeValueAsString(value));
        } catch (Exception exception) {
            throw new IllegalArgumentException("El valor JSON no es válido", exception);
        }
    }

    public List<Map<String, Object>> thresholds() {
        return repository.thresholdsByUeb();
    }

    @Transactional
    public void saveThresholds(List<JsonNode> values) {
        for (JsonNode item : values) {
            if (!item.has("uebId")) continue;
            JsonNode percentage = item.get("porcentaje");
            repository.saveThreshold(
                    item.get("uebId").asInt(),
                    percentage == null || percentage.isNull() ? null : percentage.decimalValue());
        }
    }
}