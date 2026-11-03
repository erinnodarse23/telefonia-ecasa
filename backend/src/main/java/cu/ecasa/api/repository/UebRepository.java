package cu.ecasa.api.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UebRepository {

    private final JdbcTemplate jdbcTemplate;

    public UebRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> findAll(Boolean activo) {
        if (activo == null) {
            return jdbcTemplate.queryForList("SELECT * FROM ueb ORDER BY nombre ASC");
        }
        return jdbcTemplate.queryForList("SELECT * FROM ueb WHERE activo = ? ORDER BY nombre ASC", activo);
    }

    public Optional<Map<String, Object>> findById(Integer id) {
        try {
            return Optional.of(jdbcTemplate.queryForMap("SELECT * FROM ueb WHERE id = ?", id));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public Optional<Map<String, Object>> findByCodigo(String codigo) {
        try {
            return Optional.of(jdbcTemplate.queryForMap("SELECT * FROM ueb WHERE codigo = ?", codigo));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public Map<String, Object> create(String codigo, String nombre, String descripcion) {
        return jdbcTemplate.queryForMap(
                "INSERT INTO ueb (codigo, nombre, descripcion) VALUES (?, ?, ?) RETURNING *",
                codigo, nombre, descripcion);
    }

    public Map<String, Object> update(Integer id, String codigo, String nombre, String descripcion) {
        return jdbcTemplate.queryForMap(
                "UPDATE ueb SET codigo = COALESCE(?, codigo), nombre = COALESCE(?, nombre), "
                        + "descripcion = COALESCE(?, descripcion) WHERE id = ? RETURNING *",
                codigo, nombre, descripcion, id);
    }

    public Map<String, Object> setActivo(Integer id, boolean activo) {
        return jdbcTemplate.queryForMap("UPDATE ueb SET activo = ? WHERE id = ? RETURNING *", activo, id);
    }

    public void delete(Integer id) {
        jdbcTemplate.update("DELETE FROM ueb WHERE id = ?", id);
    }

    public int countActiveEmployees(Integer id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM empleado WHERE ueb_id = ? AND activo = true", Integer.class, id);
        return count == null ? 0 : count;
    }
}