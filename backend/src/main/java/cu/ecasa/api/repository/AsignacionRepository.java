package cu.ecasa.api.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AsignacionRepository {

    private final JdbcTemplate jdbcTemplate;

    public AsignacionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> findAll(String estado) {
        String filter = "activas".equals(estado) ? " WHERE fecha_fin IS NULL" : "inactivas".equals(estado) ? " WHERE fecha_fin IS NOT NULL" : "";
        return jdbcTemplate.queryForList("SELECT * FROM asignacion" + filter + " ORDER BY fecha_inicio DESC");
    }

    public List<Map<String, Object>> findByEquipo(Integer equipoId, String estado) {
        return findByForeignKey("equipo_id", equipoId, estado);
    }

    public List<Map<String, Object>> findByEmpleado(Integer empleadoId, String estado) {
        return findByForeignKey("empleado_id", empleadoId, estado);
    }

    private List<Map<String, Object>> findByForeignKey(String column, Integer id, String estado) {
        String filter = "activas".equals(estado) ? " AND fecha_fin IS NULL" : "inactivas".equals(estado) ? " AND fecha_fin IS NOT NULL" : "";
        return jdbcTemplate.queryForList("SELECT * FROM asignacion WHERE " + column + " = ?" + filter + " ORDER BY fecha_inicio DESC", id);
    }

    public Optional<Map<String, Object>> findById(Integer id) {
        try {
            return Optional.of(jdbcTemplate.queryForMap("SELECT * FROM asignacion WHERE id = ?", id));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public boolean hasActiveByEquipo(Integer equipoId) {
        return countActive("equipo_id", equipoId) > 0;
    }

    public boolean hasActiveByEmpleado(Integer empleadoId) {
        return countActive("empleado_id", empleadoId) > 0;
    }

    private int countActive(String column, Integer id) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM asignacion WHERE " + column + " = ? AND fecha_fin IS NULL", Integer.class, id);
        return count == null ? 0 : count;
    }

    public Map<String, Object> create(Integer equipoId, Integer empleadoId, LocalDate fechaInicio, String estadoEntrega) {
        return jdbcTemplate.queryForMap(
                "INSERT INTO asignacion (equipo_id, empleado_id, fecha_inicio, estado_equipo_entrega) VALUES (?, ?, ?, ?) RETURNING *",
                equipoId, empleadoId, fechaInicio, estadoEntrega);
    }

    public Map<String, Object> finalizar(Integer id, LocalDate fechaFin, String estadoDevolucion) {
        return jdbcTemplate.queryForMap(
                "UPDATE asignacion SET fecha_fin = ?, estado_equipo_devolucion = ? WHERE id = ? RETURNING *",
                fechaFin, estadoDevolucion, id);
    }

    public void delete(Integer id) {
        jdbcTemplate.update("DELETE FROM asignacion WHERE id = ?", id);
    }
}