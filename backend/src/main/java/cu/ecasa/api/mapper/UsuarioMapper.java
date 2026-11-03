package cu.ecasa.api.mapper;

import java.util.List;

import cu.ecasa.api.dto.response.UsuarioResponse;

import java.util.Map;

/**
 * Mapper de {@code Map<String, Object>} (salida del JdbcTemplate) a
 * {@link UsuarioResponse}.
 *
 * <p>Centraliza la conversión y el manejo robusto de tipos (Number vs String
 * según el driver JDBC).
 */
public final class UsuarioMapper {

    private UsuarioMapper() {
        // Utility class
    }

    /**
     * Convierte un Map de la BD a {@link UsuarioResponse}.
     *
     * @param row mapa con columnas de la tabla {@code usuario}
     * @return DTO con los datos públicos, o {@code null} si {@code row} es null
     */
    public static UsuarioResponse toResponse(Map<String, Object> row) {
        if (row == null) {
            return null;
        }
        return new UsuarioResponse(
                toInt(row.get("id")),
                (String) row.get("login"),
                (String) row.get("rol"),
                (Boolean) row.get("activo"),
                toInt(row.get("ueb_id"))
        );
    }

    /**
     * Convierte una lista de Maps a una lista de DTOs.
     */
    public static List<UsuarioResponse> toResponseList(List<Map<String, Object>> rows) {
        return rows.stream().map(UsuarioMapper::toResponse).toList();
    }

    /**
     * Conversión robusta a {@link Integer}. Maneja {@code Number} (PostgreSQL
     * devuelve Integer/Long/BigInteger según el tipo), String numérico, vacío
     * y null.
     */
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