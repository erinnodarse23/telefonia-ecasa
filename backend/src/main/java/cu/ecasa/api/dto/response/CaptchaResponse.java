package cu.ecasa.api.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * DTO de respuesta para {@code /api/usuarios/captcha}.
 *
 * <p>Contiene el ID del captcha generado y la pregunta aritmética.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CaptchaResponse(
        String captchaId,
        String question
) {
}