package cu.ecasa.api.dto.response;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * DTO de una fila del endpoint {@code GET /api/monitoreo/estado}.
 *
 * <p>Representa el estado de consumo de un equipo en un mes concreto,
 * con su UEB, umbral, porcentaje de sobregasto y semÃ¡foro
 * ({@code verde} / {@code amarillo} / {@code rojo}).
 *
 * <p>{@code @JsonNaming(SnakeCaseStrategy)} garantiza que el JSON serializado
 * use {@code snake_case} ({@code equipo_id}, {@code numero_linea}, {@code consumo_actual}),
 * compatible con el frontend React.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MonitoreoEstadoResponse(
        Integer equipoId,
        String numeroLinea,
        Integer uebId,
        String ueb,
        BigDecimal consumoActual,
        BigDecimal umbral,
        Integer umbralUeb,
        BigDecimal porcentaje,
        String estado,
        Integer anio,
        Integer mes
) {
}