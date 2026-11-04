package cu.ecasa.api.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReporteRepository {

    private final JdbcTemplate jdbcTemplate;

    public ReporteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> responsables(Integer uebId) {
        String sql = "SELECT emp.id empleado_id, emp.nombre empleado_nombre, emp.cargo, emp.telefono empleado_telefono, "
                + "u.nombre ueb_nombre, u.codigo ueb_codigo, e.id equipo_id, e.numero_linea, e.marca, e.modelo, a.fecha_inicio "
                + "FROM empleado emp JOIN ueb u ON u.id = emp.ueb_id JOIN asignacion a ON a.empleado_id = emp.id AND a.fecha_fin IS NULL "
                + "JOIN equipo e ON e.id = a.equipo_id AND e.activo = true WHERE emp.activo = true"
                + (uebId == null ? "" : " AND u.id = ?") + " ORDER BY u.codigo, emp.nombre";
        return uebId == null ? jdbcTemplate.queryForList(sql) : jdbcTemplate.queryForList(sql, uebId);
    }

    public List<Map<String, Object>> equiposPorEstado(String estado, Integer uebId) {
        String sql = "SELECT e.* FROM equipo e WHERE e.activo = true";
        List<Object> params = new ArrayList<>();
        if (estado != null) { sql += " AND e.estado_fisico = ?"; params.add(estado); }
        if (uebId != null) {
            sql += " AND EXISTS (SELECT 1 FROM asignacion a JOIN empleado emp ON emp.id = a.empleado_id "
                    + "WHERE a.equipo_id = e.id AND a.fecha_fin IS NULL AND emp.ueb_id = ?)";
            params.add(uebId);
        }
        return jdbcTemplate.queryForList(sql + " ORDER BY e.numero_linea", params.toArray());
    }

    // ============================================================
    // Queries del módulo de Tendencias
    // ============================================================

    /**
     * Consumos mensuales del año. Usa atribución histórica (la UEB del
     * empleado asignado al cierre del mes) igual que el Dashboard.
     */
    public List<Map<String, Object>> findConsumosMensuales(Integer year, Integer uebId) {
        if (uebId == null) {
            return jdbcTemplate.queryForList(
                    "SELECT c.mes, "
                            + "SUM(c.voz_consumido) total_voz, "
                            + "SUM(c.datos_consumido) total_datos, "
                            + "SUM(c.sms_consumido) total_sms "
                            + "FROM consumo c "
                            + "WHERE c.anio = ? "
                            + "GROUP BY c.mes ORDER BY c.mes",
                    year);
        }
        return jdbcTemplate.queryForList(
                "SELECT c.mes, "
                        + "SUM(c.voz_consumido) total_voz, "
                        + "SUM(c.datos_consumido) total_datos, "
                        + "SUM(c.sms_consumido) total_sms "
                        + "FROM consumo c "
                        + "JOIN equipo e ON e.id = c.equipo_id "
                        + "JOIN asignacion a ON a.equipo_id = e.id "
                        + "  AND a.fecha_inicio <= (make_date(c.anio, c.mes, 1) + interval '1 month - 1 day')::date "
                        + "  AND (a.fecha_fin IS NULL OR a.fecha_fin >= (make_date(c.anio, c.mes, 1) + interval '1 month - 1 day')::date) "
                        + "JOIN empleado emp ON emp.id = a.empleado_id "
                        + "WHERE c.anio = ? AND emp.ueb_id = ? "
                        + "GROUP BY c.mes ORDER BY c.mes",
                year, uebId);
    }

    /**
     * Top N equipos con mayor consumo acumulado en el año.
     *
     * <p>La UEB del equipo se toma de la asignación activa actual
     * (fecha_fin IS NULL). Si no tiene asignación, sale con {@code ueb_id NULL}.
     */
    public List<Map<String, Object>> findRankingTop(Integer year, Integer uebId, int limit) {
        return findRanking(year, uebId, limit, true);
    }

    /**
     * Top N equipos con menor consumo acumulado en el año.
     * Incluye equipos con consumo 0 (sin registro en tabla consumo).
     */
    public List<Map<String, Object>> findRankingBottom(Integer year, Integer uebId, int limit) {
        return findRanking(year, uebId, limit, false);
    }

    private List<Map<String, Object>> findRanking(Integer year, Integer uebId, int limit, boolean top) {
        String filter = uebId == null ? "" : " AND u.id = ?";
        String order = top ? "DESC" : "ASC";

        String sql =
                "SELECT e.id equipo_id, e.numero_linea, u.id ueb_id, u.nombre ueb, "
                        + "  COALESCE(SUM(c.voz_consumido), 0) total_voz, "
                        + "  COALESCE(SUM(c.datos_consumido), 0) total_datos, "
                        + "  COALESCE(SUM(c.sms_consumido), 0) total_sms, "
                        + "  COALESCE(SUM(c.voz_consumido + c.datos_consumido + c.sms_consumido), 0) total "
                        + "FROM equipo e "
                        + "LEFT JOIN consumo c ON c.equipo_id = e.id AND c.anio = ? "
                        + "LEFT JOIN LATERAL ( "
                        + "  SELECT a.empleado_id FROM asignacion a "
                        + "  WHERE a.equipo_id = e.id AND a.fecha_fin IS NULL "
                        + "  ORDER BY a.fecha_inicio DESC LIMIT 1 "
                        + ") a ON true "
                        + "LEFT JOIN empleado emp ON emp.id = a.empleado_id "
                        + "LEFT JOIN ueb u ON u.id = emp.ueb_id "
                        + "WHERE e.activo = true" + filter
                        + " GROUP BY e.id, e.numero_linea, u.id, u.nombre "
                        + " ORDER BY total " + order + ", e.numero_linea "
                        + " LIMIT ?";

        List<Object> params = new ArrayList<>();
        params.add(year);
        if (uebId != null) params.add(uebId);
        params.add(limit);

        return jdbcTemplate.queryForList(sql, params.toArray());
    }

    /**
     * Comparativa anual (año actual vs anterior) agrupada por mes.
     */
    public List<Map<String, Object>> findComparativaAnual(Integer year, Integer uebId) {
        if (uebId == null) {
            return jdbcTemplate.queryForList(
                    "SELECT c.anio, c.mes, "
                            + "SUM(c.voz_consumido) total_voz, "
                            + "SUM(c.datos_consumido) total_datos, "
                            + "SUM(c.sms_consumido) total_sms "
                            + "FROM consumo c "
                            + "WHERE c.anio IN (?, ?) "
                            + "GROUP BY c.anio, c.mes ORDER BY c.anio, c.mes",
                    year, year - 1);
        }
        return jdbcTemplate.queryForList(
                "SELECT c.anio, c.mes, "
                        + "SUM(c.voz_consumido) total_voz, "
                        + "SUM(c.datos_consumido) total_datos, "
                        + "SUM(c.sms_consumido) total_sms "
                        + "FROM consumo c "
                        + "JOIN equipo e ON e.id = c.equipo_id "
                        + "JOIN asignacion a ON a.equipo_id = e.id "
                        + "  AND a.fecha_inicio <= (make_date(c.anio, c.mes, 1) + interval '1 month - 1 day')::date "
                        + "  AND (a.fecha_fin IS NULL OR a.fecha_fin >= (make_date(c.anio, c.mes, 1) + interval '1 month - 1 day')::date) "
                        + "JOIN empleado emp ON emp.id = a.empleado_id "
                        + "WHERE c.anio IN (?, ?) AND emp.ueb_id = ? "
                        + "GROUP BY c.anio, c.mes ORDER BY c.anio, c.mes",
                year, year - 1, uebId);
    }
}