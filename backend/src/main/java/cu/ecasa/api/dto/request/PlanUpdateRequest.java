package cu.ecasa.api.dto.request;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.PositiveOrZero;

/**
 * DTO de request para actualización parcial de un plan.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PlanUpdateRequest(
        @PositiveOrZero(message = "voz_plan debe ser un número no negativo")
        Integer vozPlan,

        @PositiveOrZero(message = "datos_plan debe ser un número no negativo")
        Integer datosPlan,

        @PositiveOrZero(message = "sms_plan debe ser un número no negativo")
        Integer smsPlan,

        BigDecimal cuota
) {
}