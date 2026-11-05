package cu.ecasa.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cu.ecasa.api.dto.response.DashboardIndicadorResponse;
import cu.ecasa.api.repository.DashboardRepository;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private DashboardRepository repository;

    private DashboardService service;

    @BeforeEach
    void setUp() {
        service = new DashboardService(repository);
    }

    private Map<String, Object> filaSummary(Integer uebId, String codigo, String nombre,
                                            int servicios, int lineasConDatos,
                                            String totalConsumido, String planAnual,
                                            String planMensual) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ueb_id", uebId);
        m.put("codigo", codigo);
        m.put("ueb_nombre", nombre);
        m.put("cantidad_servicios", servicios);
        m.put("lineas_con_datos", lineasConDatos);
        m.put("total_consumido", new BigDecimal(totalConsumido));
        m.put("total_voz", BigDecimal.ZERO);
        m.put("total_datos", new BigDecimal(totalConsumido));
        m.put("plan_anual", new BigDecimal(planAnual));
        m.put("plan_mensual", new BigDecimal(planMensual));
        return m;
    }

    @Test
    @DisplayName("calcularIndicadores devuelve lista vacía si no hay UEBs")
    void calcularIndicadores_vacio() {
        when(repository.findSummary(2025, null)).thenReturn(List.of());
        when(repository.findConsumosMensualesPorUeb(2025, null)).thenReturn(List.of());
        when(repository.findComparativaPorUeb(2025, null)).thenReturn(List.of());

        Map<String, Object> result = service.calcularIndicadores(2025, null);

        assertThat(result.get("anio")).isEqualTo(2025);
        assertThat((List<?>) result.get("indicadores")).isEmpty();
    }

    @Test
    @DisplayName("calcularIndicadores rellena 12 meses con ceros si no hay consumo")
    void calcularIndicadores_rellenaMesesConCeros() {
        when(repository.findSummary(2025, null)).thenReturn(List.of(
                filaSummary(1, "NC", "Nivel Central", 10, 8, "12000", "50000", "4000")));
        when(repository.findConsumosMensualesPorUeb(2025, null)).thenReturn(List.of());
        when(repository.findComparativaPorUeb(2025, null)).thenReturn(List.of());

        Map<String, Object> result = service.calcularIndicadores(2025, null);
        @SuppressWarnings("unchecked")
        List<DashboardIndicadorResponse> indicadores =
                (List<DashboardIndicadorResponse>) result.get("indicadores");

        assertThat(indicadores).hasSize(1);
        DashboardIndicadorResponse ind = indicadores.get(0);
        assertThat(ind.consumosMensuales()).hasSize(12);
        assertThat(ind.consumosMensuales().get(0).mes()).isEqualTo(1);
        assertThat(ind.consumosMensuales().get(0).total()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(ind.consumosMensuales().get(11).mes()).isEqualTo(12);
    }

    @Test
    @DisplayName("calcularIndicadores calcula porcentaje_datos correctamente")
    void calcularIndicadores_porcentajeDatos() {
        when(repository.findSummary(2025, null)).thenReturn(List.of(
                filaSummary(1, "NC", "Nivel Central", 10, 8, "1000", "10000", "1000")));
        when(repository.findConsumosMensualesPorUeb(2025, null)).thenReturn(List.of());
        when(repository.findComparativaPorUeb(2025, null)).thenReturn(List.of());

        @SuppressWarnings("unchecked")
        List<DashboardIndicadorResponse> indicadores =
                (List<DashboardIndicadorResponse>) service.calcularIndicadores(2025, null).get("indicadores");

        assertThat(indicadores.get(0).porcentajeDatos()).isEqualTo(80);
    }

    @Test
    @DisplayName("calcularIndicadores mapea los consumos mensuales del mes correcto")
    void calcularIndicadores_mapeaMesCorrecto() {
        when(repository.findSummary(2025, null)).thenReturn(List.of(
                filaSummary(1, "NC", "Nivel Central", 10, 8, "1000", "10000", "1000")));
        Map<String, Object> mes3 = new LinkedHashMap<>();
        mes3.put("ueb_id", 1);
        mes3.put("mes", 3);
        mes3.put("total_voz", new BigDecimal("100"));
        mes3.put("total_datos", new BigDecimal("200"));
        mes3.put("total_sms", new BigDecimal("10"));
        when(repository.findConsumosMensualesPorUeb(2025, null)).thenReturn(List.of(mes3));
        when(repository.findComparativaPorUeb(2025, null)).thenReturn(List.of());

        @SuppressWarnings("unchecked")
        List<DashboardIndicadorResponse> indicadores =
                (List<DashboardIndicadorResponse>) service.calcularIndicadores(2025, null).get("indicadores");

        DashboardIndicadorResponse.ConsumoMensual marzo = indicadores.get(0).consumosMensuales().get(2);
        assertThat(marzo.mes()).isEqualTo(3);
        assertThat(marzo.totalVoz()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(marzo.totalDatos()).isEqualByComparingTo(new BigDecimal("200"));
        assertThat(marzo.totalSms()).isEqualByComparingTo(new BigDecimal("10"));
        assertThat(marzo.total()).isEqualByComparingTo(new BigDecimal("310"));
    }

    @Test
    @DisplayName("calcularIndicadores construye comparativa actual vs anterior")
    void calcularIndicadores_comparativa() {
        when(repository.findSummary(2025, null)).thenReturn(List.of(
                filaSummary(1, "NC", "Nivel Central", 10, 8, "1000", "10000", "1000")));
        when(repository.findConsumosMensualesPorUeb(2025, null)).thenReturn(List.of());

        Map<String, Object> actual1 = new LinkedHashMap<>();
        actual1.put("ueb_id", 1); actual1.put("anio", 2025); actual1.put("mes", 1);
        actual1.put("total_voz", new BigDecimal("100"));
        actual1.put("total_datos", new BigDecimal("200"));
        actual1.put("total_sms", BigDecimal.ZERO);

        Map<String, Object> anterior1 = new LinkedHashMap<>();
        anterior1.put("ueb_id", 1); anterior1.put("anio", 2024); anterior1.put("mes", 1);
        anterior1.put("total_voz", new BigDecimal("50"));
        anterior1.put("total_datos", new BigDecimal("150"));
        anterior1.put("total_sms", BigDecimal.ZERO);

        when(repository.findComparativaPorUeb(2025, null)).thenReturn(List.of(actual1, anterior1));

        @SuppressWarnings("unchecked")
        List<DashboardIndicadorResponse> indicadores =
                (List<DashboardIndicadorResponse>) service.calcularIndicadores(2025, null).get("indicadores");

        DashboardIndicadorResponse.ComparativaAnual comp = indicadores.get(0).comparativa();
        assertThat(comp.anioActual()).isEqualTo(2025);
        assertThat(comp.anioAnterior()).isEqualTo(2024);
        assertThat(comp.actual()).hasSize(12);
        assertThat(comp.anterior()).hasSize(12);
        assertThat(comp.actual().get(0).total()).isEqualByComparingTo(new BigDecimal("300"));
        assertThat(comp.anterior().get(0).total()).isEqualByComparingTo(new BigDecimal("200"));
    }

    @Test
    @DisplayName("calcularIndicadores separa correctamente varias UEBs")
    void calcularIndicadores_variasUebs() {
        when(repository.findSummary(2025, null)).thenReturn(List.of(
                filaSummary(1, "NC", "Nivel Central", 10, 8, "1000", "10000", "1000"),
                filaSummary(2, "ADM", "Administrativa", 20, 10, "2000", "20000", "2000")));

        Map<String, Object> mes1Ueb1 = new LinkedHashMap<>();
        mes1Ueb1.put("ueb_id", 1); mes1Ueb1.put("mes", 1);
        mes1Ueb1.put("total_voz", new BigDecimal("100"));
        mes1Ueb1.put("total_datos", BigDecimal.ZERO); mes1Ueb1.put("total_sms", BigDecimal.ZERO);
        Map<String, Object> mes1Ueb2 = new LinkedHashMap<>();
        mes1Ueb2.put("ueb_id", 2); mes1Ueb2.put("mes", 1);
        mes1Ueb2.put("total_voz", new BigDecimal("200"));
        mes1Ueb2.put("total_datos", BigDecimal.ZERO); mes1Ueb2.put("total_sms", BigDecimal.ZERO);

        when(repository.findConsumosMensualesPorUeb(2025, null)).thenReturn(List.of(mes1Ueb1, mes1Ueb2));
        when(repository.findComparativaPorUeb(2025, null)).thenReturn(List.of());

        @SuppressWarnings("unchecked")
        List<DashboardIndicadorResponse> indicadores =
                (List<DashboardIndicadorResponse>) service.calcularIndicadores(2025, null).get("indicadores");

        assertThat(indicadores).hasSize(2);
        assertThat(indicadores.get(0).uebId()).isEqualTo(1);
        assertThat(indicadores.get(0).consumosMensuales().get(0).total()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(indicadores.get(1).uebId()).isEqualTo(2);
        assertThat(indicadores.get(1).consumosMensuales().get(0).total()).isEqualByComparingTo(new BigDecimal("200"));
    }
}