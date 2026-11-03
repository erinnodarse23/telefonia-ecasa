package cu.ecasa.api.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de request para login.
 *
 * <p>{@code rememberMe} (opcional) extiende la duración del token a 7 días.
 * {@code captchaId} y {@code captchaAnswer} (opcionales) solo se requieren
 * tras varios intentos fallidos.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record LoginRequest(
        @NotBlank(message = "El login es obligatorio")
        String login,

        @NotBlank(message = "La contraseña es obligatoria")
        String password,

        Boolean rememberMe,
        String captchaId,
        String captchaAnswer
) {
}