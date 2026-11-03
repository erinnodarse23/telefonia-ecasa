package cu.ecasa.api.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * DTO de respuesta del login exitoso.
 *
 * <p>Incluye el JWT y los datos públicos del usuario autenticado.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record LoginResponse(
        String token,
        UsuarioResponse usuario
) {
}