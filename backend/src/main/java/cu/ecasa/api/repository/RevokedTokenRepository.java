package cu.ecasa.api.repository;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de tokens JWT revocados (logout manual, desactivación de usuario).
 *
 * <p>Los tokens se almacenan en la tabla {@code tokens_invalidados} con su
 * {@code jti} (identificador único del token) y su fecha de expiración original.
 * El filtro JWT consulta este repositorio en cada request autenticado para
 * rechazar tokens revocados antes de su expiración natural.
 *
 * <p>La limpieza periódica de tokens ya expirados se realiza desde
 * {@code TokenCleanupService} con una tarea programada diaria.
 *
 * @see cu.ecasa.api.security.JwtAuthenticationFilter
 */
@Repository
public class RevokedTokenRepository {

    private static final Logger log = LoggerFactory.getLogger(RevokedTokenRepository.class);

    /** Usa EXISTS en lugar de COUNT(*): retorna en la primera fila encontrada. */
    private static final String EXISTS_SQL =
            "SELECT EXISTS(SELECT 1 FROM tokens_invalidados WHERE token_jti = ?)";

    private static final String INSERT_SQL =
            "INSERT INTO tokens_invalidados (token_jti, usuario_id, expires_at) "
                    + "VALUES (?, ?, ?) ON CONFLICT (token_jti) DO NOTHING";

    /** Borra tokens cuya fecha de expiración ya pasó (no son necesarios: JWT los rechaza igual). */
    private static final String DELETE_EXPIRED_SQL =
            "DELETE FROM tokens_invalidados WHERE expires_at < CURRENT_TIMESTAMP";

    private final JdbcTemplate jdbcTemplate;

    public RevokedTokenRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Comprueba si un token está en la lista de revocados.
     *
     * @param jti identificador único del token (claim {@code jti})
     * @return {@code true} si el token fue revocado, {@code false} en caso contrario
     */
    public boolean isRevoked(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        Boolean exists = jdbcTemplate.queryForObject(EXISTS_SQL, Boolean.class, jti);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Marca un token como revocado. Es idempotente: si el {@code jti} ya existe
     * (por un logout doble), no lanza error.
     *
     * @param jti       identificador único del token
     * @param userId    ID del usuario al que pertenece el token
     * @param expiresAt fecha de expiración original del token
     */
    public void revoke(String jti, Integer userId, Date expiresAt) {
        jdbcTemplate.update(INSERT_SQL, jti, userId, expiresAt);
        log.debug("Token revocado. jti={}, usuarioId={}", jti, userId);
    }

    /**
     * Elimina tokens revocados cuya expiración ya pasó.
     * Se invoca desde la tarea programada de limpieza.
     *
     * @return cantidad de filas eliminadas
     */
    public int deleteExpired() {
        return jdbcTemplate.update(DELETE_EXPIRED_SQL);
    }
}