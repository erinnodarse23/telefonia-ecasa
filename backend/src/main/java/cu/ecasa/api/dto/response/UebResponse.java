package cu.ecasa.api.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * DTO de respuesta con los datos de una UEB.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UebResponse(
        Integer id,
        String codigo,
        String nombre,
        String descripcion,
        Boolean activo,
        Integer umbralSobregasto
) {
}