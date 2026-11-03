package cu.ecasa.api.mapper;

import java.util.List;
import java.util.Map;

import cu.ecasa.api.dto.response.UebResponse;

/**
 * Mapper de {@code Map<String, Object>} (salida del JdbcTemplate) a
 * {@link UebResponse}.
 */
public final class UebMapper {

    private UebMapper() {
        // Utility class
    }

    public static UebResponse toResponse(Map<String, Object> row) {
        if (row == null) {
            return null;
        }
        return new UebResponse(
                toInt(row.get("id")),
                (String) row.get("codigo"),
                (String) row.get("nombre"),
                (String) row.get("descripcion"),
                (Boolean) row.get("activo"),
                toInt(row.get("umbral_sobregasto"))
        );
    }

    public static List<UebResponse> toResponseList(List<Map<String, Object>> rows) {
        return rows.stream().map(UebMapper::toResponse).toList();
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