package cu.ecasa.api.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de request para crear un usuario.
 *
 * <p>Validaciones declarativas con Jakarta Bean Validation:
 * <ul>
 *   <li>{@code login} no vacío, entre 3 y 50 caracteres</li>
 *   <li>{@code password} no vacío, mínimo 6 caracteres</li>
 *   <li>{@code rol} no vacío (la validez del rol la valida el servicio)</li>
 * </ul>
 *
 * <p>La coherencia {@code rol ↔ uebId} (JefeUnidad requiere UEB) se valida
 * en el servicio, no aquí, porque es regla de negocio.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UsuarioRequest(
        @NotBlank(message = "El login es obligatorio")
        @Size(min = 3, max = 50, message = "El login debe tener entre 3 y 50 caracteres")
        String login,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
        String password,

        @NotBlank(message = "El rol es obligatorio")
        String rol,

        Integer uebId
) {
}