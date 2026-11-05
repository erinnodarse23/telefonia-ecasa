package cu.ecasa.api.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * DTO de una fila del endpoint {@code GET /api/presupuesto}.
 *
 * <p>Representa el presupuesto anual de una UEB con su ejecución
 * (consumo acumulado del año), disponible y semáforo de ejecución:
 * <ul>
 *   <li>{@code sin_presupuesto} → asignado &le; 0</li>
 *   <li>{@code rojo} → porcentaje &gt; 100%</li>
 *   <li>{@code amarillo} → porcentaje &ge; 85%</li>
 *   <li>{@code verde} → porcentaje &lt; 85%</li>
 * </ul>
 *
 * <p>{@code umbrales} es la lista de umbrales de alerta que se disparan
 * al cruzar {@code 70}, {@code 85} y {@code 100} por ciento.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PresupuestoResponse(
        Integer uebId,
        String codigo,
        String ueb,
        Integer anio,
        BigDecimal asignado,
        BigDecimal ejecutado,
        BigDecimal disponible,
        BigDecimal porcentajeEjecucion,
        String estado,
        List<Integer> umbrales
) {
}