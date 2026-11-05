package cu.ecasa.api.dto.request;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * DTO de request para actualizar el presupuesto anual de una UEB.
 *
 * <p>Acepta tanto {@code uebId} (camelCase, como envía el frontend React)
 * como {@code ueb_id} (snake_case, convención del backend). Lo mismo para
 * {@code asignado} / {@code monto}.
 *
 * <p>El {@code @JsonNaming(SnakeCaseStrategy)} se aplica al serializar,
 * pero para deserializar necesitamos {@code @JsonAlias} porque el frontend
 * envía camelCase.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PresupuestoUpdateRequest(
        @NotNull(message = "El año es obligatorio")
        Integer anio,

        @NotNull(message = "La UEB es obligatoria")
        @JsonAlias({"uebId", "ueb_id"})
        Integer uebId,

        @NotNull(message = "El monto es obligatorio")
        @PositiveOrZero(message = "El monto debe ser un número no negativo")
        @JsonAlias({"asignado", "monto"})
        BigDecimal asignado
) {
}