package cu.ecasa.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cu.ecasa.api.repository.RevokedTokenRepository;

/**
 * Servicio de limpieza periódica de tokens JWT revocados.
 *
 * <p>Cada vez que un usuario hace logout o es desactivado, su token se añade
 * a la tabla {@code tokens_invalidados} con su fecha de expiración original.
 * Esta tabla crece indefinidamente si no se limpia.
 *
 * <p>Este servicio elimina los tokens cuya expiración natural ya pasó,
 * manteniendo la tabla pequeña y las consultas del {@code JwtAuthenticationFilter}
 * rápidas.
 *
 * <p><b>Frecuencia:</b> cada hora ({@code 0 0 * * * *}), alineado con el
 * backend Node original que usaba {@code setInterval(..., 60*60*1000)}.
 * Configurable vía la propiedad {@code app.security.token-cleanup.cron}.
 *
 * @see RevokedTokenRepository
 * @see cu.ecasa.api.security.JwtAuthenticationFilter
 */
@Service
public class TokenCleanupService {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupService.class);

    private final RevokedTokenRepository revokedTokenRepository;

    public TokenCleanupService(RevokedTokenRepository revokedTokenRepository) {
        this.revokedTokenRepository = revokedTokenRepository;
    }

    /**
     * Elimina los tokens revocados cuya expiración natural ya pasó.
     *
     * <p>Un token con {@code expires_at < now} es rechazado por
     * {@code JwtService.parse()} de todas formas, así que no necesita
     * permanecer en la tabla de revocados.
     *
     * <p>La ejecución es transaccional. Los errores se loguean pero no
     * se propagan (el job se reintentará automáticamente en la próxima
     * ejecución programada).
     */
    @Scheduled(cron = "${app.security.token-cleanup.cron:0 0 * * * *}")
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            int deleted = revokedTokenRepository.deleteExpired();

            if (deleted > 0) {
                log.info("TokenCleanupService: eliminados {} tokens revocados expirados", deleted);
            } else {
                log.debug("TokenCleanupService: no había tokens expirados para limpiar");
            }
        } catch (Exception ex) {
            log.error("TokenCleanupService: error durante la limpieza de tokens", ex);
        }
    }
}