package cu.ecasa.api.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * DTO del endpoint {@code GET /api/reportes/tendencias}.
 *
 * <p>Agrupa 4 bloques de información para la página de Tendencias del
 * frontend:
 * <ul>
 *   <li>{@code consumos_mensuales}: 12 meses del año con sus totales</li>
 *   <li>{@code ranking_top}: top 10 equipos con mayor consumo</li>
 *   <li>{@code ranking_bottom}: top 10 equipos con menor consumo</li>
 *   <li>{@code comparativa_anual}: año actual vs año anterior</li>
 * </ul>
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TendenciasResponse(
        Integer anio,
        List<ConsumoMensual> consumosMensuales,
        List<RankingItem> rankingTop,
        List<RankingItem> rankingBottom,
        ComparativaAnual comparativaAnual
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ConsumoMensual(
            Integer mes,
            BigDecimal totalVoz,
            BigDecimal totalDatos,
            BigDecimal totalSms,
            BigDecimal total
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record RankingItem(
            Integer equipoId,
            String numeroLinea,
            Integer uebId,
            String ueb,
            BigDecimal totalVoz,
            BigDecimal totalDatos,
            BigDecimal totalSms,
            BigDecimal total
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ComparativaAnual(
            Integer anioActual,
            Integer anioAnterior,
            List<ConsumoMensual> actual,
            List<ConsumoMensual> anterior
    ) {
    }
}