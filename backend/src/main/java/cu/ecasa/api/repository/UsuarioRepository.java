package cu.ecasa.api.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de usuarios del sistema.
 *
 * <p>Ofrece dos métodos de actualización según la semántica requerida:
 * <ul>
 *   <li>{@link #update(Integer, String, String, String, Boolean)} —
 *       <b>actualización parcial clásica</b>: los campos {@code null} se
 *       mantienen sin cambios (usa {@code COALESCE}). Útil para toggles
 *       como {@code activo} donde no queremos tocar el resto.</li>
 *   <li>{@link #updateWithRoleAndUeb(Integer, String, String, String, Boolean, Integer)} —
 *       <b>actualización con semántica de rol</b>: siempre escribe
 *       {@code ueb_id} con el valor recibido (incluido {@code NULL}).
 *       Necesario para cambiar de {@code JefeUnidad} a {@code Administrador}
 *       sin dejar el {@code ueb_id} huérfano.</li>
 * </ul>
 *
 * <p><b>¿Por qué dos métodos?</b> {@code COALESCE(NULL, columna)} mantiene el
 * valor anterior, lo que impide "borrar" un campo. Pero en updates parciales
 * (ej: activar/desactivar) sí queremos ese comportamiento. La regla del
 * negocio es: si el cliente envía un rol nuevo, la UEB se recalcula
 * completamente (puede ser NULL); si no envía rol, la UEB no se toca.
 *
 * @see cu.ecasa.api.service.UsuarioService
 */
@Repository
public class UsuarioRepository {

    private final JdbcTemplate jdbcTemplate;

    public UsuarioRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Map<String, Object>> findByLogin(String login) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForMap(
                    "SELECT id, login, password_hash, rol, activo, ueb_id FROM usuario WHERE login = ?",
                    login));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public Optional<Map<String, Object>> findById(Integer id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForMap(
                    "SELECT id, login, rol, activo, ueb_id FROM usuario WHERE id = ?",
                    id));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public List<Map<String, Object>> findAll() {
        return jdbcTemplate.queryForList(
                "SELECT id, login, rol, activo, ueb_id FROM usuario ORDER BY login ASC");
    }

    public Map<String, Object> create(String login, String passwordHash, String rol, Integer uebId) {
        return jdbcTemplate.queryForMap(
                "INSERT INTO usuario (login, password_hash, rol, ueb_id) VALUES (?, ?, ?, ?) "
                        + "RETURNING id, login, rol, activo, ueb_id",
                login, passwordHash, rol, uebId);
    }

    /**
     * Actualiza un usuario <b>sin tocar</b> {@code ueb_id}. Los campos
     * {@code null} se mantienen sin cambios.
     *
     * <p>Útil para: activar/desactivar, cambiar solo login, cambiar solo
     * password, etc. <b>No usar</b> si el rol cambia, porque dejaría
     * {@code ueb_id} con el valor anterior (huérfano).
     *
     * @return el usuario actualizado
     */
    public Map<String, Object> update(Integer id, String login, String passwordHash,
                                      String rol, Boolean activo) {
        return jdbcTemplate.queryForMap(
                "UPDATE usuario SET login = COALESCE(?, login), "
                        + "  password_hash = COALESCE(?, password_hash), "
                        + "  rol = COALESCE(?, rol), "
                        + "  activo = COALESCE(?, activo) "
                        + "WHERE id = ? "
                        + "RETURNING id, login, rol, activo, ueb_id",
                login, passwordHash, rol, activo, id);
    }

    /**
     * Actualiza un usuario <b>incluyendo</b> {@code ueb_id} con el valor
     * recibido (puede ser {@code null}).
     *
     * <p>Obligatorio cuando se modifica el rol, para evitar que un usuario
     * que deja de ser {@code JefeUnidad} mantenga su {@code ueb_id} asignado.
     *
     * @param uebId nuevo {@code ueb_id} (puede ser {@code null} si el rol no
     *              requiere UEB o si se quiere desasignar)
     * @return el usuario actualizado
     */
    public Map<String, Object> updateWithRoleAndUeb(Integer id, String login, String passwordHash,
                                                    String rol, Boolean activo, Integer uebId) {
        return jdbcTemplate.queryForMap(
                "UPDATE usuario SET login = COALESCE(?, login), "
                        + "  password_hash = COALESCE(?, password_hash), "
                        + "  rol = COALESCE(?, rol), "
                        + "  activo = COALESCE(?, activo), "
                        + "  ueb_id = ? "
                        + "WHERE id = ? "
                        + "RETURNING id, login, rol, activo, ueb_id",
                login, passwordHash, rol, activo, uebId, id);
    }

    public void delete(Integer id) {
        jdbcTemplate.update("DELETE FROM usuario WHERE id = ?", id);
    }
}