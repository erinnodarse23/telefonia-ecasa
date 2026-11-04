package cu.ecasa.api.dto.request;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * DTO de request para crear un consumo manual.
 *
 * <p>El frontend puede enviar los campos en camelCase ({@code equipoId})
 * o snake_case ({@code equipo_id}). El {@code @JsonNaming(SnakeCaseStrategy)}
 * se aplica al serializar; {@code @JsonAlias} permite aceptar ambas formas
 * al deserializar.
 *
 * <p>Validaciones:
 * <ul>
 *   <li>{@code equipoId}: obligatorio, positivo</li>
 *   <li>{@code anio}: obligatorio, entre 2000 y 2100</li>
 *   <li>{@code mes}: obligatorio, entre 1 y 12</li>
 *   <li>{@code vozConsumido}, {@code datosConsumido}: opcionales, ≥ 0</li>
 *   <li>{@code smsConsumido}: opcional, entero ≥ 0</li>
 * </ul>
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ConsumoCreateRequest(
        @NotNull(message = "El equipo es obligatorio")
        @Positive(message = "El equipo debe ser un ID positivo")
        @JsonAlias({"equipoId", "equipo_id"})
        Integer equipoId,

        @NotNull(message = "El año es obligatorio")
        @Min(value = 2000, message = "El año debe ser mayor o igual a 2000")
        @Max(value = 2100, message = "El año debe ser menor o igual a 2100")
        @JsonAlias({"anio", "year"})
        Integer anio,

        @NotNull(message = "El mes es obligatorio")
        @Min(value = 1, message = "El mes debe estar entre 1 y 12")
        @Max(value = 12, message = "El mes debe estar entre 1 y 12")
        @JsonAlias({"mes", "month"})
        Integer mes,

        @PositiveOrZero(message = "voz_consumido debe ser un número no negativo")
        @JsonAlias({"vozConsumido", "voz_consumido"})
        BigDecimal vozConsumido,

        @PositiveOrZero(message = "datos_consumido debe ser un número no negativo")
        @JsonAlias({"datosConsumido", "datos_consumido"})
        BigDecimal datosConsumido,

        @PositiveOrZero(message = "sms_consumido debe ser un número no negativo")
        @JsonAlias({"smsConsumido", "sms_consumido"})
        Integer smsConsumido
) {
}