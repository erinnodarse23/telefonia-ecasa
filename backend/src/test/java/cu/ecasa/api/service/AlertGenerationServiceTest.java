package cu.ecasa.api.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cu.ecasa.api.repository.AlertaRepository;
import cu.ecasa.api.repository.DashboardRepository;

/**
 * Tests unitarios para {@link AlertGenerationService}.
 *
 * <p>Verifica la lógica de evaluación de umbrales, la idempotencia y el
 * manejo defensivo de errores por UEB.
 */
@ExtendWith(MockitoExtension.class)
class AlertGenerationServiceTest {

    @Mock
    private AlertaRepository alertaRepository;

    @Mock
    private DashboardRepository dashboardRepository;

    @InjectMocks
    private AlertGenerationService alertGenerationService;

    private int anio;
    private int mes;

    @BeforeEach
    void setUp() {
        YearMonth periodo = YearMonth.now();
        anio = periodo.getYear();
        mes = periodo.getMonthValue();
    }

    private Map<String, Object> ueb(Integer id, String codigo, String nombre, int umbral) {
        return Map.of(
                "id", id,
                "codigo", codigo,
                "nombre", nombre,
                "umbral_sobregasto", umbral
        );
    }

    @Test
    @DisplayName("No genera alerta si no hay UEBs con umbral configurado")
    void noGeneraAlerta_siNoHayUebsConUmbral() {
        when(alertaRepository.findUebsConUmbral()).thenReturn(List.of());

        alertGenerationService.evaluarTodasLasUebs();

        verify(alertaRepository, never()).insert(anyInt(), anyInt(), anyInt(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("No genera alerta si el consumo está por debajo del umbral")
    void noGeneraAlerta_siConsumoBajoUmbral() {
        when(alertaRepository.findUebsConUmbral())
                .thenReturn(List.of(ueb(1, "NC", "Nivel Central", 20)));
        when(dashboardRepository.findAnnualPlan(1, anio))
                .thenReturn(Map.of("monto", new BigDecimal("10000")));
        when(alertaRepository.calcularConsumoAcumuladoPorUeb(1, anio))
                .thenReturn(new BigDecimal("1000"));

        alertGenerationService.evaluarTodasLasUebs();

        verify(alertaRepository, never()).insert(anyInt(), anyInt(), anyInt(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("Genera alerta si el consumo supera el umbral")
    void generaAlerta_siConsumoSuperaUmbral() {
        when(alertaRepository.findUebsConUmbral())
                .thenReturn(List.of(ueb(1, "NC", "Nivel Central", 20)));
        when(dashboardRepository.findAnnualPlan(1, anio))
                .thenReturn(Map.of("monto", new BigDecimal("10000")));
        when(alertaRepository.calcularConsumoAcumuladoPorUeb(1, anio))
                .thenReturn(new BigDecimal("2500"));
        when(alertaRepository.existsByPeriodo(1, anio, mes, "SOBREGASTO"))
                .thenReturn(false);

        alertGenerationService.evaluarTodasLasUebs();

        verify(alertaRepository, times(1)).insert(
                eq(1), eq(anio), eq(mes), eq("SOBREGASTO"),
                eq(new BigDecimal("25.00")), anyString());
    }

    @Test
    @DisplayName("No duplica alerta si ya existe para el mismo período")
    void noDuplica_siAlertaYaExiste() {
        when(alertaRepository.findUebsConUmbral())
                .thenReturn(List.of(ueb(1, "NC", "Nivel Central", 20)));
        when(dashboardRepository.findAnnualPlan(1, anio))
                .thenReturn(Map.of("monto", new BigDecimal("10000")));
        when(alertaRepository.calcularConsumoAcumuladoPorUeb(1, anio))
                .thenReturn(new BigDecimal("3000"));
        when(alertaRepository.existsByPeriodo(1, anio, mes, "SOBREGASTO"))
                .thenReturn(true);

        alertGenerationService.evaluarTodasLasUebs();

        verify(alertaRepository, never()).insert(anyInt(), anyInt(), anyInt(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("No evalúa UEB si no tiene plan anual configurado")
    void noEvalua_siPlanAnualEsCero() {
        when(alertaRepository.findUebsConUmbral())
                .thenReturn(List.of(ueb(1, "NC", "Nivel Central", 20)));
        when(dashboardRepository.findAnnualPlan(1, anio))
                .thenReturn(Map.of("monto", 0));

        alertGenerationService.evaluarTodasLasUebs();

        verify(alertaRepository, never()).calcularConsumoAcumuladoPorUeb(anyInt(), anyInt());
        verify(alertaRepository, never()).insert(anyInt(), anyInt(), anyInt(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("Continúa evaluando otras UEBs si una falla")
    void continua_siUnaUebFalla() {
        Map<String, Object> uebFalla = ueb(1, "NC", "Nivel Central", 20);
        Map<String, Object> uebOk = ueb(2, "ADM", "Administrativa", 30);

        when(alertaRepository.findUebsConUmbral())
                .thenReturn(List.of(uebFalla, uebOk));

        when(dashboardRepository.findAnnualPlan(1, anio))
                .thenThrow(new RuntimeException("BD caída"));
        when(dashboardRepository.findAnnualPlan(2, anio))
                .thenReturn(Map.of("monto", new BigDecimal("5000")));
        when(alertaRepository.calcularConsumoAcumuladoPorUeb(2, anio))
                .thenReturn(new BigDecimal("2000"));
        when(alertaRepository.existsByPeriodo(2, anio, mes, "SOBREGASTO"))
                .thenReturn(false);

        alertGenerationService.evaluarTodasLasUebs();

        verify(alertaRepository, times(1)).insert(
                eq(2), eq(anio), eq(mes), eq("SOBREGASTO"),
                eq(new BigDecimal("40.00")), anyString());
        verify(alertaRepository, never()).insert(
                eq(1), anyInt(), anyInt(), anyString(), any(), anyString());
    }
}