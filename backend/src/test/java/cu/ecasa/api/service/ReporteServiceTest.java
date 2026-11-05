package cu.ecasa.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import cu.ecasa.api.dto.response.TendenciasResponse;
import cu.ecasa.api.repository.ReporteRepository;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock private ReporteRepository repository;

    private ReporteService service;

    @BeforeEach
    void setUp() {
        service = new ReporteService(repository);
    }

    private Map<String, Object> mes(int mes, String voz, String datos, String sms) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("mes", mes);
        m.put("total_voz", new BigDecimal(voz));
        m.put("total_datos", new BigDecimal(datos));
        m.put("total_sms", new BigDecimal(sms));
        return m;
    }

    private Map<String, Object> rankingItem(int equipoId, String numero,
                                            Integer uebId, String ueb,
                                            String voz, String datos, String sms) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("equipo_id", equipoId);
        m.put("numero_linea", numero);
        m.put("ueb_id", uebId);
        m.put("ueb", ueb);
        m.put("total_voz", new BigDecimal(voz));
        m.put("total_datos", new BigDecimal(datos));
        m.put("total_sms", new BigDecimal(sms));
        m.put("total", new BigDecimal(voz).add(new BigDecimal(datos)).add(new BigDecimal(sms)));
        return m;
    }

    @Test
    @DisplayName("getTendencias rellena 12 meses con ceros si faltan datos")
    void getTendencias_rellenaMesesConCeros() {
        when(repository.findConsumosMensuales(2025, null)).thenReturn(List.of(
                mes(3, "100", "200", "10")));
        when(repository.findRankingTop(2025, null, 10)).thenReturn(List.of());
        when(repository.findRankingBottom(2025, null, 10)).thenReturn(List.of());
        when(repository.findComparativaAnual(2025, null)).thenReturn(List.of());

        TendenciasResponse r = service.getTendencias(2025, null);

        assertThat(r.anio()).isEqualTo(2025);
        assertThat(r.consumosMensuales()).hasSize(12);
        assertThat(r.consumosMensuales().get(0).total()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.consumosMensuales().get(2).mes()).isEqualTo(3);
        assertThat(r.consumosMensuales().get(2).total()).isEqualByComparingTo(new BigDecimal("310"));
        assertThat(r.consumosMensuales().get(11).mes()).isEqualTo(12);
    }

    @Test
    @DisplayName("getTendencias mapea ranking top y bottom")
    void getTendencias_mapeaRanking() {
        when(repository.findConsumosMensuales(2025, null)).thenReturn(List.of());
        when(repository.findRankingTop(2025, null, 10)).thenReturn(List.of(
                rankingItem(1, "53001001", 1, "NC", "100", "500", "10")));
        when(repository.findRankingBottom(2025, null, 10)).thenReturn(List.of(
                rankingItem(2, "53001002", null, null, "5", "0", "0")));
        when(repository.findComparativaAnual(2025, null)).thenReturn(List.of());

        TendenciasResponse r = service.getTendencias(2025, null);

        assertThat(r.rankingTop()).hasSize(1);
        TendenciasResponse.RankingItem top = r.rankingTop().get(0);
        assertThat(top.equipoId()).isEqualTo(1);
        assertThat(top.numeroLinea()).isEqualTo("53001001");
        assertThat(top.uebId()).isEqualTo(1);
        assertThat(top.ueb()).isEqualTo("NC");
        assertThat(top.total()).isEqualByComparingTo(new BigDecimal("610"));

        assertThat(r.rankingBottom()).hasSize(1);
        TendenciasResponse.RankingItem bottom = r.rankingBottom().get(0);
        assertThat(bottom.uebId()).isNull();
        assertThat(bottom.ueb()).isNull();
        assertThat(bottom.total()).isEqualByComparingTo(new BigDecimal("5"));
    }

    @Test
    @DisplayName("getTendencias construye comparativa actual vs anterior con 12 meses cada uno")
    void getTendencias_comparativa() {
        when(repository.findConsumosMensuales(2025, null)).thenReturn(List.of());
        when(repository.findRankingTop(2025, null, 10)).thenReturn(List.of());
        when(repository.findRankingBottom(2025, null, 10)).thenReturn(List.of());

        Map<String, Object> actualEnero = new LinkedHashMap<>();
        actualEnero.put("anio", 2025); actualEnero.put("mes", 1);
        actualEnero.put("total_voz", new BigDecimal("100"));
        actualEnero.put("total_datos", new BigDecimal("200"));
        actualEnero.put("total_sms", BigDecimal.ZERO);
        Map<String, Object> anteriorEnero = new LinkedHashMap<>();
        anteriorEnero.put("anio", 2024); anteriorEnero.put("mes", 1);
        anteriorEnero.put("total_voz", new BigDecimal("50"));
        anteriorEnero.put("total_datos", new BigDecimal("100"));
        anteriorEnero.put("total_sms", BigDecimal.ZERO);
        when(repository.findComparativaAnual(2025, null)).thenReturn(List.of(actualEnero, anteriorEnero));

        TendenciasResponse r = service.getTendencias(2025, null);

        TendenciasResponse.ComparativaAnual comp = r.comparativaAnual();
        assertThat(comp.anioActual()).isEqualTo(2025);
        assertThat(comp.anioAnterior()).isEqualTo(2024);
        assertThat(comp.actual()).hasSize(12);
        assertThat(comp.anterior()).hasSize(12);
        assertThat(comp.actual().get(0).total()).isEqualByComparingTo(new BigDecimal("300"));
        assertThat(comp.anterior().get(0).total()).isEqualByComparingTo(new BigDecimal("150"));
    }

    @Test
    @DisplayName("equiposPorEstado valida el estado")
    void equiposPorEstado_estadoInvalido() {
        assertThatThrownBy(() -> service.equiposPorEstado("roto", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Estado inválido");
    }
}