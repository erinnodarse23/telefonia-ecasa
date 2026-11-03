package cu.ecasa.api.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * DTO de respuesta con los datos públicos de un usuario.
 *
 * <p>Nunca incluye {@code password_hash} ni datos sensibles.
 * La anotación {@code @JsonNaming} garantiza que el JSON serializado use
 * {@code snake_case} ({@code ueb_id}), compatible con el frontend Node existente.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UsuarioResponse(
        Integer id,
        String login,
        String rol,
        Boolean activo,
        Integer uebId
) {
}