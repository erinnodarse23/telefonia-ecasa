package cu.ecasa.api.dto.request;

import java.time.LocalDate;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.NotNull;

/**
 * DTO de request para finalizar una asignación.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record FinalizarAsignacionRequest(
        @NotNull(message = "La fecha de fin es obligatoria")
        LocalDate fechaFin,

        String estadoEquipoDevolucion
) {
}