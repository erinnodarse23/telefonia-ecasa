package cu.ecasa.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cu.ecasa.api.repository.RevokedTokenRepository;
import cu.ecasa.api.service.TokenCleanupService;

/**
 * Tests unitarios para {@link TokenCleanupService}.
 */
@ExtendWith(MockitoExtension.class)
class TokenCleanupServiceTest {

    @Mock
    private RevokedTokenRepository revokedTokenRepository;

    @InjectMocks
    private TokenCleanupService tokenCleanupService;

    @Test
    @DisplayName("cleanupExpiredTokens() llama a deleteExpired() del repositorio")
    void cleanupExpiredTokens_callsRepository() {
        when(revokedTokenRepository.deleteExpired()).thenReturn(5);

        tokenCleanupService.cleanupExpiredTokens();

        verify(revokedTokenRepository).deleteExpired();
    }

    @Test
    @DisplayName("cleanupExpiredTokens() no lanza excepción si el repositorio falla")
    void cleanupExpiredTokens_swallowsException() {
        when(revokedTokenRepository.deleteExpired())
                .thenThrow(new RuntimeException("BD caída"));

        // No debe lanzar: el job debe seguir vivo aunque falle una vez
        tokenCleanupService.cleanupExpiredTokens();

        verify(revokedTokenRepository).deleteExpired();
    }

    @Test
    @DisplayName("cleanupExpiredTokens() funciona cuando no hay nada que limpiar")
    void cleanupExpiredTokens_noOpWhenEmpty() {
        when(revokedTokenRepository.deleteExpired()).thenReturn(0);

        tokenCleanupService.cleanupExpiredTokens();

        verify(revokedTokenRepository).deleteExpired();
    }
}