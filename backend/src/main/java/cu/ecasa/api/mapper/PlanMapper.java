package cu.ecasa.api.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import cu.ecasa.api.dto.response.PlanResponse;

/**
 * Mapper de {@code Map<String, Object>} a {@link PlanResponse}.
 *
 * <p>Maneja la conversión robusta de tipos comunes que devuelve PostgreSQL:
 * {@code Integer/Long} para ids, {@code Numeric→BigDecimal} para cuota,
 * {@code Date/Timestamp→LocalDate} para fechas.
 */
public final class PlanMapper {

    private PlanMapper() {
    }

    public static PlanResponse toResponse(Map<String, Object> row) {
        if (row == null) {
            return null;
        }
        return new PlanResponse(
                toInt(row.get("id")),
                toInt(row.get("equipo_id")),
                toInt(row.get("voz_plan")),
                toInt(row.get("datos_plan")),
                toInt(row.get("sms_plan")),
                toBigDecimal(row.get("cuota")),
                toLocalDate(row.get("fecha_inicio")),
                toLocalDate(row.get("fecha_fin"))
        );
    }

    public static List<PlanResponse> toResponseList(List<Map<String, Object>> rows) {
        return rows.stream().map(PlanMapper::toResponse).toList();
    }

    private static Integer toInt(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.intValue();
        try { return Integer.parseInt(value.toString().trim()); } catch (NumberFormatException e) { return null; }
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try { return new BigDecimal(value.toString()); } catch (NumberFormatException e) { return null; }
    }

    private static LocalDate toLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate ld) return ld;
        if (value instanceof java.sql.Date sqlDate) return sqlDate.toLocalDate();
        return LocalDate.parse(value.toString());
    }
}