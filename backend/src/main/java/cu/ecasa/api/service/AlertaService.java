package cu.ecasa.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
 * <p><b>Nota:</b> la generación automática de alertas por sobregasto mensual
 * vive en {@link AlertGenerationService} con su propio {@code @Scheduled}.
 * Este servicio expone además {@link #verificarPresupuestoAnual(Integer, Integer)}
 * para evaluar alertas de presupuesto anual (70%, 85%, 100%).
 *
 * @see AlertaRepository
 * @see AlertGenerationService
 * @see DashboardService
 */
@Service
public class AlertaService {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final List<Integer> UMBRALES_PRESUPUESTO = List.of(70, 85, 100);
    /** Mes "diciembre" usado como convención para alertas anuales. */
    private static final int MES_ANUAL = 12;

    private final AlertaRepository repository;
    private final DashboardService dashboardService;

    public AlertaService(AlertaRepository repository, DashboardService dashboardService) {
        this.repository = repository;
        this.dashboardService = dashboardService;
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

    /**
     * Evalúa las alertas de presupuesto anual (umbrales 70%, 85%, 100%) para
     * una UEB o para todas.
     *
     * <p>Para cada UEB con presupuesto asignado &gt; 0:
     * <ol>
     *   <li>Calcula el porcentaje de ejecución (ejecutado / asignado * 100)</li>
     *   <li>Por cada umbral que se haya superado, inserta una alerta
     *       {@code PRESUPUESTO_70}, {@code PRESUPUESTO_85} o {@code PRESUPUESTO_100}</li>
     *   <li>Es idempotente: usa {@link AlertaRepository#existsByPeriodo}
     *       para no duplicar alertas del mismo período</li>
     * </ol>
     *
     * <p>El "mes" de la alerta es {@code 12} (diciembre) por convención: las
     * alertas de presupuesto se consideran anuales.
     *
     * @param anio  año del presupuesto
     * @param uebId ID de la UEB (null = todas)
     */
    @Transactional
    public void verificarPresupuestoAnual(Integer anio, Integer uebId) {
        List<Map<String, Object>> filas = dashboardService.summary(anio, uebId);
        for (Map<String, Object> fila : filas) {
            BigDecimal asignado = decimal(fila.get("plan_anual"));
            if (asignado.signum() <= 0) continue;

            BigDecimal ejecutado = decimal(fila.get("total_consumido"));
            BigDecimal porcentaje = ejecutado.multiply(CIEN)
                    .divide(asignado, 2, RoundingMode.HALF_UP);

            Integer uebIdFila = ((Number) fila.get("ueb_id")).intValue();
            String codigo = (String) fila.get("codigo");
            String nombre = (String) fila.get("ueb_nombre");

            for (Integer umbral : UMBRALES_PRESUPUESTO) {
                if (porcentaje.compareTo(BigDecimal.valueOf(umbral)) < 0) continue;

                String tipo = "PRESUPUESTO_" + umbral;
                if (repository.existsByPeriodo(uebIdFila, anio, MES_ANUAL, tipo)) continue;

                String mensaje = String.format(
                        "%s (%s) alcanzó el %d%% de ejecución presupuestaria en %d. "
                                + "Ejecutado: %.2f vs Asignado: %.2f (%.2f%%).",
                        nombre, codigo, umbral, anio, ejecutado, asignado, porcentaje);

                repository.insert(uebIdFila, anio, MES_ANUAL, tipo, porcentaje, mensaje);
            }
        }
    }

    private Map<String, Object> require(Map<String, Object> value) {
        if (value == null) throw new ResourceNotFoundException("Alerta no encontrada");
        return value;
    }

    private BigDecimal decimal(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }
}