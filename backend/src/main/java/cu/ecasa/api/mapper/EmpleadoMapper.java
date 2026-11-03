package cu.ecasa.api.mapper;

import java.util.List;
import java.util.Map;

import cu.ecasa.api.dto.response.EmpleadoResponse;

/**
 * Mapper de {@code Map<String, Object>} a {@link EmpleadoResponse}.
 */
public final class EmpleadoMapper {

    private EmpleadoMapper() {
    }

    public static EmpleadoResponse toResponse(Map<String, Object> row) {
        if (row == null) {
            return null;
        }
        return new EmpleadoResponse(
                toInt(row.get("id")),
                (String) row.get("nombre"),
                (String) row.get("cargo"),
                (String) row.get("departamento"),
                (String) row.get("telefono"),
                toInt(row.get("ueb_id")),
                (Boolean) row.get("activo")
        );
    }

    public static List<EmpleadoResponse> toResponseList(List<Map<String, Object>> rows) {
        return rows.stream().map(EmpleadoMapper::toResponse).toList();
    }

    private static Integer toInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s) {
            String trimmed = s.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            try {
                return Integer.parseInt(trimmed);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}