package cu.ecasa.api.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de request para crear un equipo.
 *
 * <p>Validaciones declarativas:
 * <ul>
 *   <li>{@code numeroLinea}: obligatorio, máximo 20 caracteres</li>
 *   <li>{@code marca}, {@code modelo}: opcionales</li>
 *   <li>{@code estadoFisico}: opcional, si viene debe ser uno de
 *       {@code nuevo}, {@code bueno}, {@code dañado}, {@code de baja}</li>
 *   <li>{@code observaciones}: opcional</li>
 * </ul>
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record EquipoRequest(
        @NotBlank(message = "El número de línea es obligatorio")
        @Size(max = 20, message = "El número de línea no puede exceder 20 caracteres")
        String numeroLinea,

        String marca,
        String modelo,
        String estadoFisico,
        String observaciones
) {
}