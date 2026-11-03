package cu.ecasa.api.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * DTO de respuesta para {@code /api/auth/verify}.
 *
 * <p>Confirma que el token es válido y devuelve los datos actuales del usuario.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record VerifyResponse(
        Boolean valid,
        UsuarioResponse usuario
) {
}