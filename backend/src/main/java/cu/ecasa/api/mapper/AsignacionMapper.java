package cu.ecasa.api.mapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import cu.ecasa.api.dto.response.AsignacionResponse;

/**
 * Mapper de {@code Map<String, Object>} a {@link AsignacionResponse}.
 */
public final class AsignacionMapper {

    private AsignacionMapper() {
    }

    public static AsignacionResponse toResponse(Map<String, Object> row) {
        if (row == null) {
            return null;
        }
        return new AsignacionResponse(
                toInt(row.get("id")),
                toInt(row.get("equipo_id")),
                toInt(row.get("empleado_id")),
                toLocalDate(row.get("fecha_inicio")),
                toLocalDate(row.get("fecha_fin")),
                (String) row.get("estado_equipo_entrega"),
                (String) row.get("estado_equipo_devolucion")
        );
    }

    public static List<AsignacionResponse> toResponseList(List<Map<String, Object>> rows) {
        return rows.stream().map(AsignacionMapper::toResponse).toList();
    }

    private static Integer toInt(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.intValue();
        try { return Integer.parseInt(value.toString().trim()); } catch (NumberFormatException e) { return null; }
    }

    private static LocalDate toLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate ld) return ld;
        if (value instanceof java.sql.Date sqlDate) return sqlDate.toLocalDate();
        return LocalDate.parse(value.toString());
    }
}