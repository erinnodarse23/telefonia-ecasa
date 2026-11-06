package cu.ecasa.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cu.ecasa.api.repository.RevokedTokenRepository;

/**
 * Servicio de limpieza periÃ³dica de tokens JWT revocados.
 *
 * <p>Cada vez que un usuario hace logout o es desactivado, su token se aÃ±ade
 * a la tabla {@code tokens_invalidados} con su fecha de expiraciÃ³n original.
 * Esta tabla crece indefinidamente si no se limpia.
 *
 * <p>Este servicio elimina los tokens cuya expiraciÃ³n natural ya pasÃ³,
 * manteniendo la tabla pequeÃ±a y las consultas del {@code JwtAuthenticationFilter}
 * rÃ¡pidas.
 *
 * <p><b>Frecuencia:</b> cada hora ({@code 0 0 * * * *}), alineado con el
 * que ejecutaba la limpieza periódicamente.
 * Configurable vÃ­a la propiedad {@code app.security.token-cleanup.cron}.
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
     * Elimina los tokens revocados cuya expiraciÃ³n natural ya pasÃ³.
     *
     * <p>Un token con {@code expires_at < now} es rechazado por
     * {@code JwtService.parse()} de todas formas, asÃ­ que no necesita
     * permanecer en la tabla de revocados.
     *
     * <p>La ejecuciÃ³n es transaccional. Los errores se loguean pero no
     * se propagan (el job se reintentarÃ¡ automÃ¡ticamente en la prÃ³xima
     * ejecuciÃ³n programada).
     */
    @Scheduled(cron = "${app.security.token-cleanup.cron:0 0 * * * *}")
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            int deleted = revokedTokenRepository.deleteExpired();

            if (deleted > 0) {
                log.info("TokenCleanupService: eliminados {} tokens revocados expirados", deleted);
            } else {
                log.debug("TokenCleanupService: no habÃ­a tokens expirados para limpiar");
            }
        } catch (Exception ex) {
            log.error("TokenCleanupService: error durante la limpieza de tokens", ex);
        }
    }
}