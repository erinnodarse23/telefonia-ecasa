package cu.ecasa.api.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO de request para crear o actualizar una UEB.
 *
 * <p>Validaciones declarativas (Jakarta Bean Validation):
 * <ul>
 *   <li>{@code codigo}: obligatorio, 2-10 caracteres, alfanumérico</li>
 *   <li>{@code nombre}: obligatorio, máximo 100 caracteres</li>
 *   <li>{@code descripcion}: opcional</li>
 * </ul>
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UebRequest(
        @NotBlank(message = "El código es obligatorio")
        @Size(min = 2, max = 10, message = "El código debe tener entre 2 y 10 caracteres")
        @Pattern(regexp = "[a-zA-Z0-9]+", message = "El código debe ser alfanumérico")
        String codigo,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        String nombre,

        String descripcion
) {
}