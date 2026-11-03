package cu.ecasa.api.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * DTO de request para crear un plan.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PlanRequest(
        @NotNull(message = "El equipo es obligatorio")
        Integer equipoId,

        @PositiveOrZero(message = "voz_plan debe ser un número no negativo")
        Integer vozPlan,

        @PositiveOrZero(message = "datos_plan debe ser un número no negativo")
        Integer datosPlan,

        @PositiveOrZero(message = "sms_plan debe ser un número no negativo")
        Integer smsPlan,

        BigDecimal cuota,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate fechaInicio
) {
}