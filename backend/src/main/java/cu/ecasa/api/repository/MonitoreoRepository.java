package cu.ecasa.api.repository;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de consultas para el módulo de monitoreo.
 *
 * <p>Devuelve el estado de consumo de cada equipo activo en un mes concreto.
 * La consulta usa {@code LEFT JOIN LATERAL} de PostgreSQL para obtener:
 * <ul>
 *   <li>La <b>última asignación activa</b> del equipo (para saber su UEB actual)</li>
 *   <li>El <b>último plan activo</b> del equipo (para saber su cuota mensual)</li>
 *   <li>El <b>consumo total</b> del mes (voz + datos + sms)</li>
 * </ul>
 *
 * <p>Todos los equipos se devuelven aunque no tengan asignación, plan o consumo
 * (los campos faltantes salen como {@code null} o {@code 0} según la expresión
 * {@code COALESCE}).
 *
 * @see cu.ecasa.api.service.MonitoreoService
 */
@Repository
public class MonitoreoRepository {

    private final JdbcTemplate jdbcTemplate;

    public MonitoreoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Consulta el estado de monitoreo por equipo.
     *
     * @param anio  año a consultar
     * @param mes   mes a consultar (1-12)
     * @param uebId ID de UEB para filtrar (null = todas las UEBs)
     * @return lista de mapas con los campos crudos de la consulta
     */
    public List<Map<String, Object>> findEstado(Integer anio, Integer mes, Integer uebId) {
        String sql =
                "SELECT "
              + "  e.id AS equipo_id, "
              + "  e.numero_linea, "
              + "  u.id AS ueb_id, "
              + "  u.nombre AS ueb, "
              + "  COALESCE(c.consumo_actual, 0) AS consumo_actual, "
              + "  COALESCE(p.cuota, 0) AS umbral, "
              + "  u.umbral_sobregasto AS umbral_ueb "
              + "FROM equipo e "
              + "LEFT JOIN LATERAL ( "
              + "  SELECT a.empleado_id FROM asignacion a "
              + "  WHERE a.equipo_id = e.id AND a.fecha_fin IS NULL "
              + "  ORDER BY a.fecha_inicio DESC LIMIT 1 "
              + ") a ON true "
              + "LEFT JOIN empleado emp ON emp.id = a.empleado_id AND emp.activo = true "
              + "LEFT JOIN ueb u ON u.id = emp.ueb_id "
              + "LEFT JOIN LATERAL ( "
              + "  SELECT p.cuota FROM plan p "
              + "  WHERE p.equipo_id = e.id AND p.fecha_fin IS NULL "
              + "  ORDER BY p.fecha_inicio DESC LIMIT 1 "
              + ") p ON true "
              + "LEFT JOIN LATERAL ( "
              + "  SELECT SUM(c.voz_consumido + c.datos_consumido + c.sms_consumido) AS consumo_actual "
              + "  FROM consumo c "
              + "  WHERE c.equipo_id = e.id AND c.anio = ? AND c.mes = ? "
              + ") c ON true "
              + "WHERE e.activo = true "
              + (uebId != null ? "AND u.id = ? " : "")
              + "ORDER BY e.numero_linea";

        Object[] params = uebId != null
                ? new Object[] { anio, mes, uebId }
                : new Object[] { anio, mes };

        return jdbcTemplate.queryForList(sql, params);
    }
}