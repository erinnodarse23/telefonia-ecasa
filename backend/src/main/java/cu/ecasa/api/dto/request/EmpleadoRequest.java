package cu.ecasa.api.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO de request para crear un empleado.
 *
 * <p>Validaciones declarativas:
 * <ul>
 *   <li>{@code nombre}: obligatorio, máximo 200 caracteres</li>
 *   <li>{@code telefono}: opcional, si viene debe cumplir el patrón</li>
 *   <li>{@code uebId}: obligatorio</li>
 * </ul>
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record EmpleadoRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
        String nombre,

        String cargo,
        String departamento,

        @Pattern(regexp = "^[+]?[\\d\\s()\\-]{6,20}$",
                message = "El formato del teléfono no es válido")
        String telefono,

        @NotNull(message = "La UEB es obligatoria")
        Integer uebId
) {
}