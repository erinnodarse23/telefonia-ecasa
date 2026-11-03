package cu.ecasa.api.dto.response;

import java.time.LocalDate;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * DTO de respuesta con los datos de una asignación.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AsignacionResponse(
        Integer id,
        Integer equipoId,
        Integer empleadoId,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String estadoEquipoEntrega,
        String estadoEquipoDevolucion
) {
}