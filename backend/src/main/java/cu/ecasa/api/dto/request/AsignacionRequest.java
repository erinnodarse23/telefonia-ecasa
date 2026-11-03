package cu.ecasa.api.dto.request;

import java.time.LocalDate;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.NotNull;

/**
 * DTO de request para crear una asignación de equipo a empleado.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AsignacionRequest(
        @NotNull(message = "El equipo es obligatorio")
        Integer equipoId,

        @NotNull(message = "El empleado es obligatorio")
        Integer empleadoId,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate fechaInicio,

        String estadoEquipoEntrega
) {
}