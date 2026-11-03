package cu.ecasa.api.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * DTO de respuesta con los datos de un empleado.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record EmpleadoResponse(
        Integer id,
        String nombre,
        String cargo,
        String departamento,
        String telefono,
        Integer uebId,
        Boolean activo
) {
}