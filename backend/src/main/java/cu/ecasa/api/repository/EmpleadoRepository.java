package cu.ecasa.api.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de empleados.
 *
 * @see cu.ecasa.api.service.EmpleadoService
 */
@Repository
public class EmpleadoRepository {

    private final JdbcTemplate jdbcTemplate;

    public EmpleadoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> findAll(Boolean activo) {
        String sql = "SELECT * FROM empleado" + (activo == null ? "" : " WHERE activo = ?") + " ORDER BY nombre ASC";
        return activo == null ? jdbcTemplate.queryForList(sql) : jdbcTemplate.queryForList(sql, activo);
    }

    public List<Map<String, Object>> findByUeb(Integer uebId, Boolean activo) {
        String sql = "SELECT * FROM empleado WHERE ueb_id = ?" + (activo == null ? "" : " AND activo = ?") + " ORDER BY nombre ASC";
        return activo == null ? jdbcTemplate.queryForList(sql, uebId) : jdbcTemplate.queryForList(sql, uebId, activo);
    }

    public Optional<Map<String, Object>> findById(Integer id) {
        try {
            return Optional.of(jdbcTemplate.queryForMap("SELECT * FROM empleado WHERE id = ?", id));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public Map<String, Object> create(String nombre, String cargo, String departamento, String telefono, Integer uebId) {
        return jdbcTemplate.queryForMap(
                "INSERT INTO empleado (nombre, cargo, departamento, telefono, ueb_id) VALUES (?, ?, ?, ?, ?) RETURNING *",
                nombre, cargo, departamento, telefono, uebId);
    }

    /**
     * Actualiza un empleado con actualización parcial clásica (COALESCE).
     * Los campos {@code null} no se tocan.
     *
     * <p><b>Nota:</b> para borrar el teléfono o desasignar la UEB, usar
     * {@link #updateAllowingNulls(Integer, String, String, String, String, Integer)}.
     */
    public Map<String, Object> update(Integer id, String nombre, String cargo, String departamento,
                                      String telefono, Integer uebId) {
        return jdbcTemplate.queryForMap(
                "UPDATE empleado SET nombre = COALESCE(?, nombre), "
                        + "  cargo = COALESCE(?, cargo), "
                        + "  departamento = COALESCE(?, departamento), "
                        + "  telefono = COALESCE(?, telefono), "
                        + "  ueb_id = COALESCE(?, ueb_id) "
                        + "WHERE id = ? RETURNING *",
                nombre, cargo, departamento, telefono, uebId, id);
    }

    /**
     * Actualiza un empleado permitiendo poner {@code NULL} en {@code telefono}
     * y {@code ueb_id}.
     *
     * <p>Se llama desde el servicio cuando los campos llegan explícitamente
     * en el request (aunque sea con valor vacío) y el negocio requiere
     * poder limpiarlos.
     *
     * @return el empleado actualizado
     */
    public Map<String, Object> updateAllowingNulls(Integer id, String nombre, String cargo,
                                                   String departamento, String telefono, Integer uebId) {
        return jdbcTemplate.queryForMap(
                "UPDATE empleado SET nombre = COALESCE(?, nombre), "
                        + "  cargo = COALESCE(?, cargo), "
                        + "  departamento = COALESCE(?, departamento), "
                        + "  telefono = ?, "
                        + "  ueb_id = ? "
                        + "WHERE id = ? RETURNING *",
                nombre, cargo, departamento, telefono, uebId, id);
    }

    public Map<String, Object> setActivo(Integer id, boolean activo) {
        return jdbcTemplate.queryForMap("UPDATE empleado SET activo = ? WHERE id = ? RETURNING *", activo, id);
    }

    public Map<String, Object> updateTelefono(Integer id, String telefono) {
        return jdbcTemplate.queryForMap("UPDATE empleado SET telefono = ? WHERE id = ? RETURNING *", telefono, id);
    }

    public int countActiveAssignments(Integer id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM asignacion WHERE empleado_id = ? AND fecha_fin IS NULL", Integer.class, id);
        return count == null ? 0 : count;
    }

    public void delete(Integer id) {
        jdbcTemplate.update("DELETE FROM empleado WHERE id = ?", id);
    }
}