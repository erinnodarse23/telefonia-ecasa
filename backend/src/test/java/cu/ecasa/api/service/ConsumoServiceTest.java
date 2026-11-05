package cu.ecasa.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.dao.DataIntegrityViolationException;

import cu.ecasa.api.exceptions.ResourceNotFoundException;
import cu.ecasa.api.repository.ConsumoRepository;
import cu.ecasa.api.repository.EquipoRepository;

/**
 * Tests unitarios para {@link ConsumoService}.
 *
 * <p>Cubre:
 * <ul>
 *   <li>{@code update}: enganche con {@link AlertGenerationService} y manejo defensivo</li>
 *   <li>{@code create}: validaciones estrictas y detección de duplicados</li>
 * </ul>
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
    private Map<String, Object> equipoExistente;

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
        equipoExistente = Map.of("id", 10, "numero_linea", "53001001");
    }

    // ============================================================
    // Tests de update() (existentes)
    // ============================================================

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

        doThrow(new RuntimeException("BD caída"))
                .when(alertGenerationService).evaluarTodasLasUebs();

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

    // ============================================================
    // Tests de create() (nuevos)
    // ============================================================

    @Test
    @DisplayName("create() inserta consumo válido y reevalúa alertas")
    void create_insertaConsumoValido() {
        when(equipoRepository.findById(10)).thenReturn(Optional.of(equipoExistente));
        when(consumoRepository.existsByEquipoAndPeriodo(10, 2025, 10)).thenReturn(false);
        Map<String, Object> created = Map.of(
                "id", 2, "equipo_id", 10, "anio", 2025, "mes", 10,
                "voz_consumido", new BigDecimal("100"),
                "datos_consumido", new BigDecimal("500"),
                "sms_consumido", 10);
        when(consumoRepository.insertOrUpdate(10, 2025, 10,
                new BigDecimal("100"), new BigDecimal("500"), 10)).thenReturn(created);

        Map<String, Object> result = consumoService.create(10, 2025, 10,
                new BigDecimal("100"), new BigDecimal("500"), 10);

        assertThat(result.get("id")).isEqualTo(2);
        verify(alertGenerationService).evaluarTodasLasUebs();
    }

    @Test
    @DisplayName("create() NO inserta si el equipo no existe")
    void create_lanzaExcepcionSiEquipoNoExiste() {
        when(equipoRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consumoService.create(99, 2025, 10,
                new BigDecimal("100"), null, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Equipo");

        verify(consumoRepository, never()).insertOrUpdate(any(), any(), any(), any(), any(), any());
        verify(alertGenerationService, never()).evaluarTodasLasUebs();
    }

    @Test
    @DisplayName("create() lanza excepción si el año está fuera de rango")
    void create_lanzaExcepcionSiAnioFueraDeRango() {
        when(equipoRepository.findById(10)).thenReturn(Optional.of(equipoExistente));

        assertThatThrownBy(() -> consumoService.create(10, 1999, 10,
                new BigDecimal("100"), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("año");

        assertThatThrownBy(() -> consumoService.create(10, 2101, 10,
                new BigDecimal("100"), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("año");

        verify(consumoRepository, never()).insertOrUpdate(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("create() lanza excepción si el mes es inválido")
    void create_lanzaExcepcionSiMesInvalido() {
        when(equipoRepository.findById(10)).thenReturn(Optional.of(equipoExistente));

        assertThatThrownBy(() -> consumoService.create(10, 2025, 0,
                new BigDecimal("100"), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mes");

        assertThatThrownBy(() -> consumoService.create(10, 2025, 13,
                new BigDecimal("100"), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mes");

        verify(consumoRepository, never()).insertOrUpdate(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("create() lanza 409 (DataIntegrityViolationException) si ya existe")
    void create_lanzaExcepcionSiYaExiste() {
        when(equipoRepository.findById(10)).thenReturn(Optional.of(equipoExistente));
        when(consumoRepository.existsByEquipoAndPeriodo(10, 2025, 10)).thenReturn(true);

        assertThatThrownBy(() -> consumoService.create(10, 2025, 10,
                new BigDecimal("100"), null, null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("Ya existe un consumo");

        verify(consumoRepository, never()).insertOrUpdate(any(), any(), any(), any(), any(), any());
        verify(alertGenerationService, never()).evaluarTodasLasUebs();
    }

    @Test
    @DisplayName("create() lanza excepción si valores son negativos")
    void create_lanzaExcepcionSiValoresNegativos() {
        when(equipoRepository.findById(10)).thenReturn(Optional.of(equipoExistente));

        assertThatThrownBy(() -> consumoService.create(10, 2025, 10,
                new BigDecimal("-1"), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("voz_consumido");

        assertThatThrownBy(() -> consumoService.create(10, 2025, 10,
                null, null, -5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sms_consumido");
    }

    @Test
    @DisplayName("create() tolera fallo en alertas (no propaga)")
    void create_toleraFalloEnAlertas() {
        when(equipoRepository.findById(10)).thenReturn(Optional.of(equipoExistente));
        when(consumoRepository.existsByEquipoAndPeriodo(10, 2025, 10)).thenReturn(false);
        Map<String, Object> created = Map.of(
                "id", 2, "equipo_id", 10, "anio", 2025, "mes", 10,
                "voz_consumido", new BigDecimal("100"),
                "datos_consumido", BigDecimal.ZERO,
                "sms_consumido", 0);
        when(consumoRepository.insertOrUpdate(eq(10), eq(2025), eq(10),
                eq(new BigDecimal("100")), eq(BigDecimal.ZERO), eq(0))).thenReturn(created);

        doThrow(new RuntimeException("BD caída"))
                .when(alertGenerationService).evaluarTodasLasUebs();

        Map<String, Object> result = consumoService.create(10, 2025, 10,
                new BigDecimal("100"), BigDecimal.ZERO, 0);

        assertThat(result.get("id")).isEqualTo(2);
    }
}