package cu.ecasa.api.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO de request para actualizar un empleado (actualización parcial).
 *
 * <p>Todos los campos son opcionales. Reglas especiales:
 * <ul>
 *   <li>{@code telefono = ""} (vacío) → borra el teléfono del empleado</li>
 *   <li>{@code uebId = 0} → desasigna la UEB del empleado</li>
 * </ul>
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record EmpleadoUpdateRequest(
        @Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
        String nombre,

        String cargo,
        String departamento,

        @Pattern(regexp = "^[+]?[\\d\\s()\\-]{6,20}$",
                message = "El formato del teléfono no es válido")
        String telefono,

        Integer uebId
) {
}