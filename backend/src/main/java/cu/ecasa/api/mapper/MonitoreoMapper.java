package cu.ecasa.api.mapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import cu.ecasa.api.dto.response.MonitoreoEstadoResponse;

/**
 * Mapper de {@code Map<String,Object>} (fila SQL) a {@link MonitoreoEstadoResponse}.
 *
 * <p>Calcula el porcentaje de sobregasto ({@code consumo_actual / umbral * 100})
 * y el semáforo de estado ({@code verde} / {@code amarillo} / {@code rojo})
 * según los umbrales del negocio:
 * <ul>
 *   <li>{@code > 85%} → {@code rojo}</li>
 *   <li>{@code >= 70%} → {@code amarillo}</li>
 *   <li>{@code < 70%} → {@code verde}</li>
 * </ul>
 *
 * <p>Si el equipo no tiene plan ({@code umbral = 0}), el porcentaje se devuelve
 * como {@code 0} y el estado como {@code verde} (no se puede calcular sobregasto
 * sin cuota de referencia).
 */
@Component
public class MonitoreoMapper {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private static final BigDecimal UMBRAL_ROJO = BigDecimal.valueOf(85);
    private static final BigDecimal UMBRAL_AMARILLO = BigDecimal.valueOf(70);

    /**
     * Convierte una fila SQL en un DTO de respuesta.
     */
    public MonitoreoEstadoResponse toResponse(Map<String, Object> row, Integer anio, Integer mes) {
        BigDecimal consumo = decimal(row.get("consumo_actual"));
        BigDecimal umbral = decimal(row.get("umbral"));
        BigDecimal porcentaje = calcularPorcentaje(consumo, umbral);

        return new MonitoreoEstadoResponse(
                intOrNull(row.get("equipo_id")),
                (String) row.get("numero_linea"),
                intOrNull(row.get("ueb_id")),
                (String) row.get("ueb"),
                consumo,
                umbral,
                intOrNull(row.get("umbral_ueb")),
                porcentaje,
                calcularEstado(porcentaje),
                anio,
                mes
        );
    }

    /**
     * Convierte una lista de filas SQL en lista de DTOs.
     */
    public List<MonitoreoEstadoResponse> toResponseList(List<Map<String, Object>> rows, Integer anio, Integer mes) {
        return rows.stream().map(row -> toResponse(row, anio, mes)).toList();
    }

    /**
     * Calcula el porcentaje de sobregasto.
     * <p>Si el umbral es {@code 0} o {@code null}, devuelve {@code 0}.
     */
    private BigDecimal calcularPorcentaje(BigDecimal consumo, BigDecimal umbral) {
        if (umbral == null || umbral.signum() <= 0) return BigDecimal.ZERO;
        return consumo.multiply(CIEN).divide(umbral, 2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula el semáforo según el porcentaje.
     */
    private String calcularEstado(BigDecimal porcentaje) {
        if (porcentaje.compareTo(UMBRAL_ROJO) > 0) return "rojo";
        if (porcentaje.compareTo(UMBRAL_AMARILLO) >= 0) return "amarillo";
        return "verde";
    }

    private BigDecimal decimal(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }

    private Integer intOrNull(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }
}