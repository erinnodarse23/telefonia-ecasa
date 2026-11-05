package cu.ecasa.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cu.ecasa.api.dto.response.PresupuestoResponse;

@ExtendWith(MockitoExtension.class)
class PresupuestoServiceTest {

    @Mock private DashboardService dashboardService;
    @Mock private ConfiguracionService configuracionService;
    @Mock private AlertaService alertaService;

    private PresupuestoService service;

    @BeforeEach
    void setUp() {
        service = new PresupuestoService(dashboardService, configuracionService, alertaService);
    }

    private Map<String, Object> fila(Integer uebId, String codigo, String nombre,
                                     String planAnual, String totalConsumido) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ueb_id", uebId);
        m.put("codigo", codigo);
        m.put("ueb_nombre", nombre);
        m.put("plan_anual", planAnual == null ? null : new BigDecimal(planAnual));
        m.put("total_consumido", totalConsumido == null ? null : new BigDecimal(totalConsumido));
        return m;
    }

    @Test
    void getPresupuesto_estadoVerde_cuandoEjecucionMenor85() {
        when(dashboardService.summary(2025, null))
                .thenReturn(List.of(fila(1, "NC", "Nivel Central", "10000", "5000")));

        PresupuestoResponse r = service.getPresupuesto(2025, null).get(0);

        assertThat(r.uebId()).isEqualTo(1);
        assertThat(r.estado()).isEqualTo("verde");
        assertThat(r.porcentajeEjecucion()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(r.disponible()).isEqualByComparingTo(new BigDecimal("5000"));
    }

    @Test
    void getPresupuesto_estadoAmarillo_cuandoEjecucionEntre85Y100() {
        when(dashboardService.summary(2025, null))
                .thenReturn(List.of(fila(1, "NC", "Nivel Central", "10000", "9000")));

        PresupuestoResponse r = service.getPresupuesto(2025, null).get(0);

        assertThat(r.estado()).isEqualTo("amarillo");
        assertThat(r.porcentajeEjecucion()).isEqualByComparingTo(new BigDecimal("90.00"));
    }

    @Test
    void getPresupuesto_estadoRojo_cuandoEjecucionSupera100() {
        when(dashboardService.summary(2025, null))
                .thenReturn(List.of(fila(1, "NC", "Nivel Central", "10000", "12000")));

        PresupuestoResponse r = service.getPresupuesto(2025, null).get(0);

        assertThat(r.estado()).isEqualTo("rojo");
        assertThat(r.porcentajeEjecucion()).isEqualByComparingTo(new BigDecimal("120.00"));
        assertThat(r.disponible()).isEqualByComparingTo(new BigDecimal("-2000"));
    }

    @Test
    void getPresupuesto_sinPresupuesto_cuandoAsignadoCero() {
        when(dashboardService.summary(2025, null))
                .thenReturn(List.of(fila(1, "NC", "Nivel Central", "0", "5000")));

        PresupuestoResponse r = service.getPresupuesto(2025, null).get(0);

        assertThat(r.estado()).isEqualTo("sin_presupuesto");
        assertThat(r.porcentajeEjecucion()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getPresupuesto_devuelveListaVacia_cuandoNoHayFilas() {
        when(dashboardService.summary(2025, null)).thenReturn(List.of());

        assertThat(service.getPresupuesto(2025, null)).isEmpty();
    }

    @Test
    void setAsignado_guardaYVerificaAlertas() {
        BigDecimal nuevoMonto = new BigDecimal("15000");
        when(dashboardService.summary(2025, 1))
                .thenReturn(List.of(fila(1, "NC", "Nivel Central", "15000", "1000")));

        PresupuestoResponse r = service.setAsignado(2025, 1, nuevoMonto);

        verify(configuracionService).saveAnnualPlan(eq(1), eq(2025), eq(nuevoMonto));
        verify(alertaService).verificarPresupuestoAnual(eq(2025), eq(1));
        assertThat(r.asignado()).isEqualByComparingTo(nuevoMonto);
        assertThat(r.uebId()).isEqualTo(1);
    }
}