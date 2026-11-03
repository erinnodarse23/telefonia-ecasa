package cu.ecasa.api.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EquipoRepository {

    private final JdbcTemplate jdbcTemplate;

    public EquipoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> findAll(Boolean activo) {
        String sql = "SELECT * FROM equipo" + (activo == null ? "" : " WHERE activo = ?") + " ORDER BY numero_linea ASC";
        return activo == null ? jdbcTemplate.queryForList(sql) : jdbcTemplate.queryForList(sql, activo);
    }

    public List<Map<String, Object>> findByEstado(String estado) {
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT id, numero_linea, marca, modelo, estado_fisico, observaciones FROM equipo "
                        + "WHERE estado_fisico = ? AND activo = true ORDER BY numero_linea", estado);
    }

    public Optional<Map<String, Object>> findById(Integer id) {
        try {
            return Optional.of(jdbcTemplate.queryForMap("SELECT * FROM equipo WHERE id = ?", id));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public Optional<Map<String, Object>> findByNumero(String numero) {
        try {
            return Optional.of(jdbcTemplate.queryForMap("SELECT * FROM equipo WHERE numero_linea = ?", numero));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public Map<String, Object> create(String numero, String marca, String modelo, String estado, String observaciones) {
        return jdbcTemplate.queryForMap(
                "INSERT INTO equipo (numero_linea, marca, modelo, estado_fisico, observaciones) VALUES (?, ?, ?, ?, ?) RETURNING *",
                numero, marca, modelo, estado, observaciones);
    }

    public Map<String, Object> update(Integer id, String numero, String marca, String modelo, String estado, String observaciones) {
        return jdbcTemplate.queryForMap(
                "UPDATE equipo SET numero_linea = COALESCE(?, numero_linea), marca = COALESCE(?, marca), "
                        + "modelo = COALESCE(?, modelo), estado_fisico = COALESCE(?, estado_fisico), "
                        + "observaciones = COALESCE(?, observaciones) WHERE id = ? RETURNING *",
                numero, marca, modelo, estado, observaciones, id);
    }

    public Map<String, Object> setActivo(Integer id, boolean activo) {
        return jdbcTemplate.queryForMap("UPDATE equipo SET activo = ? WHERE id = ? RETURNING *", activo, id);
    }

    public int countActiveAssignments(Integer id) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM asignacion WHERE equipo_id = ? AND fecha_fin IS NULL", Integer.class, id);
        return count == null ? 0 : count;
    }

    public int countActivePlans(Integer id) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM plan WHERE equipo_id = ? AND fecha_fin IS NULL", Integer.class, id);
        return count == null ? 0 : count;
    }

    public void delete(Integer id) {
        jdbcTemplate.update("DELETE FROM equipo WHERE id = ?", id);
    }
}