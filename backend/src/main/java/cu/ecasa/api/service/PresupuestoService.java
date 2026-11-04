package cu.ecasa.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import cu.ecasa.api.dto.response.PresupuestoResponse;

/**
 * Servicio del módulo de presupuesto anual por UEB.
 *
 * <p>Reutiliza {@link DashboardService#summary(Integer, Integer)} para
 * obtener el plan anual asignado y el consumo acumulado del año. La lógica
 * de negocio aquí es:
 * <ul>
 *   <li>Mapear los campos del dashboard a los del presupuesto
 *       ({@code plan_anual → asignado}, {@code total_consumido → ejecutado})</li>
 *   <li>Calcular disponible y porcentaje de ejecución</li>
 *   <li>Asignar el semáforo de estado</li>
 * </ul>
 *
 * <p><b>Nota sobre transacciones:</b> {@link #setAsignado} NO es
 * {@code @Transactional} a propósito. Si falla la verificación de alertas
 * posterior, el presupuesto ya quedó guardado (comportamiento igual al
 * backend Node original).
 *
 * @see DashboardService
 * @see ConfiguracionService
 * @see AlertaService
 */
@Service
public class PresupuestoService {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final List<Integer> UMBRALES = List.of(70, 85, 100);

    private final DashboardService dashboardService;
    private final ConfiguracionService configuracionService;
    private final AlertaService alertaService;

    public PresupuestoService(DashboardService dashboardService,
                              ConfiguracionService configuracionService,
                              AlertaService alertaService) {
        this.dashboardService = dashboardService;
        this.configuracionService = configuracionService;
        this.alertaService = alertaService;
    }

    /**
     * Devuelve el presupuesto anual por UEB.
     *
     * @param anio  año a consultar
     * @param uebId ID de UEB (null = todas)
     * @return lista de {@link PresupuestoResponse} por UEB
     */
    public List<PresupuestoResponse> getPresupuesto(Integer anio, Integer uebId) {
        return dashboardService.summary(anio, uebId).stream().map(fila -> {
            BigDecimal asignado = decimal(fila.get("plan_anual"));
            BigDecimal ejecutado = decimal(fila.get("total_consumido"));
            BigDecimal disponible = asignado.subtract(ejecutado);
            BigDecimal porcentaje = asignado.signum() > 0
                    ? ejecutado.multiply(CIEN).divide(asignado, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            return new PresupuestoResponse(
                    ((Number) fila.get("ueb_id")).intValue(),
                    (String) fila.get("codigo"),
                    (String) fila.get("ueb_nombre"),
                    anio,
                    asignado,
                    ejecutado,
                    disponible,
                    porcentaje,
                    calcularEstado(asignado, porcentaje),
                    UMBRALES
            );
        }).toList();
    }

    /**
     * Actualiza el presupuesto anual de una UEB y reevalúa las alertas.
     *
     * <p>El orden es:
     * <ol>
     *   <li>Guarda el nuevo monto en {@code plan_anual} (UPSERT)</li>
     *   <li>Verifica y genera alertas de presupuesto (70%, 85%, 100%)</li>
     * </ol>
     *
     * <p>Si la verificación falla, el presupuesto ya está guardado; el error
     * se propaga al cliente (comportamiento igual al Node).
     *
     * @param anio     año del presupuesto
     * @param uebId    ID de la UEB
     * @param asignado monto anual asignado (>= 0)
     * @return fila de presupuesto actualizada
     */
    public PresupuestoResponse setAsignado(Integer anio, Integer uebId, BigDecimal asignado) {
        configuracionService.saveAnnualPlan(uebId, anio, asignado);
        alertaService.verificarPresupuestoAnual(anio, uebId);
        return getPresupuesto(anio, uebId).stream()
                .filter(r -> r.uebId().equals(uebId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No se pudo recuperar el presupuesto recién guardado"));
    }

    private String calcularEstado(BigDecimal asignado, BigDecimal porcentaje) {
        if (asignado.signum() <= 0) return "sin_presupuesto";
        if (porcentaje.compareTo(CIEN) > 0) return "rojo";
        if (porcentaje.compareTo(BigDecimal.valueOf(85)) >= 0) return "amarillo";
        return "verde";
    }

    private BigDecimal decimal(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }
}