package cu.ecasa.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * DTO de respuesta con los datos de un plan.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PlanResponse(
        Integer id,
        Integer equipoId,
        Integer vozPlan,
        Integer datosPlan,
        Integer smsPlan,
        BigDecimal cuota,
        LocalDate fechaInicio,
        LocalDate fechaFin
) {
}