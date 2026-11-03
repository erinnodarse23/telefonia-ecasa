package cu.ecasa.api.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cu.ecasa.api.repository.AlertaRepository;
import cu.ecasa.api.exceptions.ResourceNotFoundException;

/**
 * Servicio de gestión de alertas (consulta y mutaciones).
 *
 * <p><b>Atomicidad:</b> todas las operaciones de escritura son transaccionales.
 * Las operaciones de lectura no lo necesitan (son {@code SELECT} únicos).
 *
 * <p><b>Nota:</b> la generación automática de alertas vive en
 * {@link AlertGenerationService} con su propio {@code @Scheduled}.
 *
 * @see AlertaRepository
 * @see AlertGenerationService
 */
@Service
public class AlertaService {

    private final AlertaRepository repository;

    public AlertaService(AlertaRepository repository) {
        this.repository = repository;
    }

    public Map<String, Object> list(String tipo, Boolean leida, Boolean soloActivas,
                                    Integer uebId, int pagina, int limite) {
        int safePage = Math.max(1, pagina);
        int safeLimit = Math.max(1, Math.min(limite, 500));
        long total = repository.count(tipo, leida, soloActivas, uebId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("datos", repository.find(tipo, leida, soloActivas, uebId,
                safeLimit, (safePage - 1) * safeLimit));
        result.put("total", total);
        result.put("pagina", safePage);
        result.put("totalPaginas", (int) Math.ceil((double) total / safeLimit));
        result.put("limite", safeLimit);
        return result;
    }

    public long unreadCount(Integer uebId) {
        return repository.count(null, false, true, uebId);
    }

    public List<Map<String, Object>> recent(int limit, Integer uebId) {
        return repository.recent(Math.max(1, Math.min(limit, 100)), uebId);
    }

    @Transactional
    public Map<String, Object> markRead(Integer id) {
        return require(repository.markRead(id));
    }

    @Transactional
    public Map<String, Object> discard(Integer id) {
        return require(repository.discard(id));
    }

    @Transactional
    public int markAllRead(Integer uebId) {
        return repository.markAllRead(uebId);
    }

    @Transactional
    public int archiveAll(Integer uebId) {
        return repository.archiveAll(uebId);
    }

    private Map<String, Object> require(Map<String, Object> value) {
        if (value == null) throw new ResourceNotFoundException("Alerta no encontrada");
        return value;
    }
}