package cu.ecasa.api.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * DTO de un indicador del endpoint {@code GET /api/dashboard/indicadores}.
 *
 * <p>Representa el estado consolidado de una UEB en un año concreto:
 * líneas, datos, consumos mensuales, plan anual, promedios y comparativa
 * con el año anterior.
 *
 * <p>Todos los campos monetarios son {@link BigDecimal} para evitar
 * problemas de precisión. El JSON se serializa en snake_case por
 * {@code @JsonNaming}.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record DashboardIndicadorResponse(
        Integer uebId,
        String codigo,
        String ueb,
        Integer totalLineas,
        Integer lineasConDatos,
        Integer porcentajeDatos,
        List<ConsumoMensual> consumosMensuales,
        BigDecimal totalAnual,
        BigDecimal planAnual,
        BigDecimal planMensual,
        BigDecimal promedioReal,
        BigDecimal estimadoCierre,
        BigDecimal diferencia,
        BigDecimal porcentajeSobregasto,
        BigDecimal pctSobregasto,
        ComparativaAnual comparativa
) {

    /**
     * Consumo de un mes concreto. El campo {@code total} es la suma de
     * {@code totalVoz + totalDatos + totalSms} y se precalcula para que el
     * frontend no tenga que hacerlo.
     */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ConsumoMensual(
            Integer mes,
            BigDecimal totalVoz,
            BigDecimal totalDatos,
            BigDecimal totalSms,
            BigDecimal total
    ) {
    }

    /**
     * Comparativa del año actual contra el anterior. Incluye los 12 meses
     * de cada año (rellenados con ceros si falta algún mes) para que el
     * frontend pueda graficar sin tener que rellenar huecos.
     */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ComparativaAnual(
            Integer anioActual,
            Integer anioAnterior,
            List<ConsumoMensual> actual,
            List<ConsumoMensual> anterior
    ) {
    }
}