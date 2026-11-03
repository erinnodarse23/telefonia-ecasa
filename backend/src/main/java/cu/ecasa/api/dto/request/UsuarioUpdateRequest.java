package cu.ecasa.api.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.Size;

/**
 * DTO de request para actualizar un usuario.
 *
 * <p>Todos los campos son opcionales (actualización parcial). Si {@code rol}
 * está presente, el servicio recalcula {@code uebId} automáticamente para
 * evitar huérfanos al cambiar de {@code JefeUnidad} a otro rol.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UsuarioUpdateRequest(
        @Size(min = 3, max = 50, message = "El login debe tener entre 3 y 50 caracteres")
        String login,

        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
        String password,

        String rol,
        Boolean activo,
        Integer uebId
) {
}