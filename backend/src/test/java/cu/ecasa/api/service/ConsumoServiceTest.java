package cu.ecasa.api.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cu.ecasa.api.repository.ConsumoRepository;
import cu.ecasa.api.repository.EquipoRepository;

/**
 * Tests unitarios para {@link ConsumoService}.
 *
 * <p>Verifica el enganche con {@link AlertGenerationService} y el manejo
 * defensivo de errores.
 */
@ExtendWith(MockitoExtension.class)
class ConsumoServiceTest {

    @Mock
    private ConsumoRepository consumoRepository;

    @Mock
    private EquipoRepository equipoRepository;

    @Mock
    private AlertGenerationService alertGenerationService;

    @InjectMocks
    private ConsumoService consumoService;

    private Map<String, Object> consumoExistente;

    @BeforeEach
    void setUp() {
        consumoExistente = Map.of(
                "id", 1,
                "equipo_id", 10,
                "anio", 2025,
                "mes", 10,
                "voz_consumido", new BigDecimal("100"),
                "datos_consumido", new BigDecimal("500"),
                "sms_consumido", 10
        );
    }

    @Test
    @DisplayName("update() llama a alertGenerationService al actualizar")
    void update_llamaAlertGeneration() {
        when(consumoRepository.findById(1)).thenReturn(Optional.of(consumoExistente));
        when(consumoRepository.update(1, new BigDecimal("200"), null, null))
                .thenReturn(consumoExistente);

        consumoService.update(1, new BigDecimal("200"), null, null);

        verify(alertGenerationService, times(1)).evaluarTodasLasUebs();
    }

    @Test
    @DisplayName("update() no rompe si la generación de alertas falla")
    void update_toleraFalloEnAlertas() {
        when(consumoRepository.findById(1)).thenReturn(Optional.of(consumoExistente));
        when(consumoRepository.update(anyInt(), any(), any(), any()))
                .thenReturn(consumoExistente);

        // ✅ FIX: para métodos void se usa doThrow(...).when(...) en lugar de
        // when(...).thenThrow(...). El método evaluarTodasLasUebs() es void.
        doThrow(new RuntimeException("BD caída"))
                .when(alertGenerationService).evaluarTodasLasUebs();

        // No debe propagar la excepción al llamador
        consumoService.update(1, new BigDecimal("200"), null, null);

        verify(alertGenerationService, times(1)).evaluarTodasLasUebs();
    }

    @Test
    @DisplayName("update() NO llama a alertGenerationService si la validación falla")
    void update_noLlamaAlertGeneration_siValidacionFalla() {
        try {
            consumoService.update(1, null, null, null);
        } catch (IllegalArgumentException ignored) { /* esperado */ }

        verify(alertGenerationService, never()).evaluarTodasLasUebs();
    }
}