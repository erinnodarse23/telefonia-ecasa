package cu.ecasa.api.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.Size;

/**
 * DTO de request para actualizar un equipo (actualización parcial).
 *
 * <p>Todos los campos son opcionales. Los campos {@code null} no se tocan.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record EquipoUpdateRequest(
        @Size(max = 20, message = "El número de línea no puede exceder 20 caracteres")
        String numeroLinea,

        String marca,
        String modelo,
        String estadoFisico,
        String observaciones
) {
}