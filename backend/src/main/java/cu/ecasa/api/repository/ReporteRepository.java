package cu.ecasa.api.repository;

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
        List<Object> params = new java.util.ArrayList<>();
        if (estado != null) { sql += " AND e.estado_fisico = ?"; params.add(estado); }
        if (uebId != null) {
            sql += " AND EXISTS (SELECT 1 FROM asignacion a JOIN empleado emp ON emp.id = a.empleado_id "
                    + "WHERE a.equipo_id = e.id AND a.fecha_fin IS NULL AND emp.ueb_id = ?)";
            params.add(uebId);
        }
        return jdbcTemplate.queryForList(sql + " ORDER BY e.numero_linea", params.toArray());
    }
}