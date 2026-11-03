package cu.ecasa.api.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * DTO de respuesta con los datos de un equipo.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record EquipoResponse(
        Integer id,
        String numeroLinea,
        String marca,
        String modelo,
        String estadoFisico,
        String observaciones,
        Boolean activo
) {
}