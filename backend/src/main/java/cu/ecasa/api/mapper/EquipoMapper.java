package cu.ecasa.api.mapper;

import java.util.List;
import java.util.Map;

import cu.ecasa.api.dto.response.EquipoResponse;

/**
 * Mapper de {@code Map<String, Object>} a {@link EquipoResponse}.
 */
public final class EquipoMapper {

    private EquipoMapper() {
    }

    public static EquipoResponse toResponse(Map<String, Object> row) {
        if (row == null) {
            return null;
        }
        return new EquipoResponse(
                toInt(row.get("id")),
                (String) row.get("numero_linea"),
                (String) row.get("marca"),
                (String) row.get("modelo"),
                (String) row.get("estado_fisico"),
                (String) row.get("observaciones"),
                (Boolean) row.get("activo")
        );
    }

    public static List<EquipoResponse> toResponseList(List<Map<String, Object>> rows) {
        return rows.stream().map(EquipoMapper::toResponse).toList();
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