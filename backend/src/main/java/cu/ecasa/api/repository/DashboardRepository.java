package cu.ecasa.api.repository;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de consultas agregadas para el dashboard.
 *
 * <p>Incluye cálculos mensuales, comparativas año-contra-año, resumen por UEB
 * y consulta de plan anual. Todas las consultas con filtro por UEB usan
 * <b>atribución histórica correcta</b>: cada consumo se atribuye a la UEB
 * que tenía el equipo asignado en el <b>último día</b> del mes del consumo,
 * no a la UEB actual.
 *
 * <p><b>Convención de atribución:</b> un consumo del mes {@code (anio, mes)}
 * pertenece a la UEB de la asignación que estaba activa al cierre del mes.
 * Si el equipo cambió de UEB el día 15, el consumo completo del mes se
 * atribuye al nuevo destino (política de "responsable al cierre del período",
 * estándar en reportes financieros mensuales).
 *
 * <p><b>Expresión PostgreSQL:</b> {@code make_date(anio, mes, 1) + interval '1 month - 1 day'}
 * devuelve el último día del mes (equivalente a {@code last_day} de MySQL).
 *
 * @see cu.ecasa.api.service.DashboardService
 */
@Repository
public class DashboardRepository {

    private final JdbcTemplate jdbcTemplate;

    public DashboardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Integer> findConsumptionYears() {
        return jdbcTemplate.query(
                "SELECT DISTINCT anio FROM consumo ORDER BY anio DESC",
                (rs, rowNum) -> rs.getInt("anio"));
    }

    /**
     * Totales mensuales de consumo para un año.
     *
     * @param year  año a consultar
     * @param uebId ID de la UEB (null para todas las UEBs)
     * @return lista de mapas con {@code mes}, {@code total_voz}, {@code total_datos}, {@code total_sms}
     */
    public List<Map<String, Object>> findMonthlyTotals(Integer year, Integer uebId) {
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
     * Comparativa de consumo entre un año y el anterior.
     *
     * @param year  año base (se compara con {@code year - 1})
     * @param uebId ID de la UEB (null para todas)
     * @return lista de mapas con {@code anio}, {@code mes} y los 3 totales
     */
    public List<Map<String, Object>> findAnnualComparison(Integer year, Integer uebId) {
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

    /**
     * Resumen agregado del año por UEB.
     *
     * <p>Combina dos subqueries independientes:
     * <ul>
     *   <li><b>serv</b>: servicios y plan mensual con la asignación <i>actual</i>
     *       (sin fecha_fin). Refleja el estado <i>hoy</i>.</li>
     *   <li><b>cons</b>: consumo histórico por UEB, atribuido por fecha de
     *       asignación al cierre del mes. Refleja el consumo real de la UEB
     *       a lo largo del año.</li>
     * </ul>
     *
     * @param year  año a consultar
     * @param uebId ID de UEB (null para todas)
     * @return lista de mapas con el resumen por UEB
     */
    public List<Map<String, Object>> findSummary(Integer year, Integer uebId) {
        String filter = uebId == null ? "" : " AND u.id = ?";

        String sql =
                "SELECT u.id ueb_id, u.codigo, u.nombre ueb_nombre, "
                        + "  COALESCE(serv.cantidad_servicios, 0) cantidad_servicios, "
                        + "  COALESCE(serv.lineas_con_datos, 0) lineas_con_datos, "
                        + "  COALESCE(serv.plan_mensual, 0) plan_mensual, "
                        + "  COALESCE(cons.total_voz, 0) total_voz, "
                        + "  COALESCE(cons.total_datos, 0) total_datos, "
                        + "  COALESCE(cons.total_voz + cons.total_datos, 0) total_consumido "
                        + "FROM ueb u "
                        + "LEFT JOIN ( "
                        + "    SELECT emp.ueb_id, "
                        + "           COUNT(DISTINCT e.id) cantidad_servicios, "
                        + "           COUNT(DISTINCT CASE WHEN p.id IS NOT NULL AND p.datos_plan > 0 THEN e.id END) lineas_con_datos, "
                        + "           SUM(p.cuota) plan_mensual "
                        + "    FROM empleado emp "
                        + "    JOIN asignacion a ON a.empleado_id = emp.id AND a.fecha_fin IS NULL "
                        + "    JOIN equipo e ON e.id = a.equipo_id AND e.activo = true "
                        + "    LEFT JOIN plan p ON p.equipo_id = e.id AND p.fecha_fin IS NULL "
                        + "    WHERE emp.activo = true "
                        + "    GROUP BY emp.ueb_id "
                        + ") serv ON serv.ueb_id = u.id "
                        + "LEFT JOIN ( "
                        + "    SELECT emp2.ueb_id, "
                        + "           SUM(c.voz_consumido) total_voz, "
                        + "           SUM(c.datos_consumido) total_datos "
                        + "    FROM consumo c "
                        + "    JOIN equipo e2 ON e2.id = c.equipo_id "
                        + "    JOIN asignacion a2 ON a2.equipo_id = e2.id "
                        + "      AND a2.fecha_inicio <= (make_date(c.anio, c.mes, 1) + interval '1 month - 1 day')::date "
                        + "      AND (a2.fecha_fin IS NULL OR a2.fecha_fin >= (make_date(c.anio, c.mes, 1) + interval '1 month - 1 day')::date) "
                        + "    JOIN empleado emp2 ON emp2.id = a2.empleado_id "
                        + "    WHERE c.anio = ? "
                        + "    GROUP BY emp2.ueb_id "
                        + ") cons ON cons.ueb_id = u.id "
                        + "WHERE u.activo = true" + filter
                        + " ORDER BY u.codigo";

        return uebId == null
                ? jdbcTemplate.queryForList(sql, year)
                : jdbcTemplate.queryForList(sql, year, uebId);
    }

    /**
     * Obtiene el monto del plan anual de una UEB para un año.
     *
     * @param uebId ID de la UEB
     * @param year  año
     * @return mapa con {@code monto}; si no existe, {@code monto = 0}
     */
    public Map<String, Object> findAnnualPlan(Integer uebId, Integer year) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT monto FROM plan_anual WHERE ueb_id = ? AND anio = ?",
                uebId, year);
        return rows.isEmpty() ? Map.of("monto", 0) : rows.get(0);
    }

    /**
     * Consumos mensuales agrupados por UEB (12 filas por UEB, una por mes
     * con datos; los meses sin consumo no aparecen y el servicio los
     * rellena con ceros).
     *
     * <p>Usa la misma atribución histórica que {@link #findMonthlyTotals}:
     * el consumo de cada mes se atribuye a la UEB de la asignación activa
     * al cierre del mes.
     *
     * @param year  año a consultar
     * @param uebId ID de UEB (null para todas)
     * @return lista de mapas con {@code ueb_id}, {@code mes}, {@code total_voz},
     *         {@code total_datos}, {@code total_sms}
     */
    public List<Map<String, Object>> findConsumosMensualesPorUeb(Integer year, Integer uebId) {
        String filter = uebId == null ? "" : " AND emp.ueb_id = ?";

        String sql =
                "SELECT emp.ueb_id, c.mes, "
                        + "SUM(c.voz_consumido) total_voz, "
                        + "SUM(c.datos_consumido) total_datos, "
                        + "SUM(c.sms_consumido) total_sms "
                        + "FROM consumo c "
                        + "JOIN equipo e ON e.id = c.equipo_id "
                        + "JOIN asignacion a ON a.equipo_id = e.id "
                        + "  AND a.fecha_inicio <= (make_date(c.anio, c.mes, 1) + interval '1 month - 1 day')::date "
                        + "  AND (a.fecha_fin IS NULL OR a.fecha_fin >= (make_date(c.anio, c.mes, 1) + interval '1 month - 1 day')::date) "
                        + "JOIN empleado emp ON emp.id = a.empleado_id "
                        + "WHERE c.anio = ?" + filter
                        + " GROUP BY emp.ueb_id, c.mes "
                        + "ORDER BY emp.ueb_id, c.mes";

        return uebId == null
                ? jdbcTemplate.queryForList(sql, year)
                : jdbcTemplate.queryForList(sql, year, uebId);
    }

    /**
     * Comparativa anual (año actual vs anterior) agrupada por UEB y mes.
     *
     * <p>Devuelve hasta 24 filas por UEB (12 del año actual + 12 del anterior,
     * solo los meses con datos). El servicio rellena con ceros los meses
     * faltantes.
     *
     * @param year  año base (se compara con {@code year - 1})
     * @param uebId ID de UEB (null para todas)
     * @return lista de mapas con {@code ueb_id}, {@code anio}, {@code mes},
     *         {@code total_voz}, {@code total_datos}, {@code total_sms}
     */
    public List<Map<String, Object>> findComparativaPorUeb(Integer year, Integer uebId) {
        String filter = uebId == null ? "" : " AND emp.ueb_id = ?";

        String sql =
                "SELECT emp.ueb_id, c.anio, c.mes, "
                        + "SUM(c.voz_consumido) total_voz, "
                        + "SUM(c.datos_consumido) total_datos, "
                        + "SUM(c.sms_consumido) total_sms "
                        + "FROM consumo c "
                        + "JOIN equipo e ON e.id = c.equipo_id "
                        + "JOIN asignacion a ON a.equipo_id = e.id "
                        + "  AND a.fecha_inicio <= (make_date(c.anio, c.mes, 1) + interval '1 month - 1 day')::date "
                        + "  AND (a.fecha_fin IS NULL OR a.fecha_fin >= (make_date(c.anio, c.mes, 1) + interval '1 month - 1 day')::date) "
                        + "JOIN empleado emp ON emp.id = a.empleado_id "
                        + "WHERE c.anio IN (?, ?)" + filter
                        + " GROUP BY emp.ueb_id, c.anio, c.mes "
                        + "ORDER BY emp.ueb_id, c.anio, c.mes";

        return uebId == null
                ? jdbcTemplate.queryForList(sql, year, year - 1)
                : jdbcTemplate.queryForList(sql, year, year - 1, uebId);
    }
}