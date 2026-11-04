package cu.ecasa.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cu.ecasa.api.dto.response.MonitoreoEstadoResponse;
import cu.ecasa.api.mapper.MonitoreoMapper;
import cu.ecasa.api.repository.MonitoreoRepository;

@ExtendWith(MockitoExtension.class)
class MonitoreoServiceTest {

    @Mock
    private MonitoreoRepository repository;

    private MonitoreoService service;

    @BeforeEach
    void setUp() {
        service = new MonitoreoService(repository, new MonitoreoMapper());
    }

    @Test
    void getEstado_conAnioYMesExplicitos_llamaAlRepository() {
        when(repository.findEstado(2026, 3, null)).thenReturn(List.of());

        List<MonitoreoEstadoResponse> result = service.getEstado(2026, 3, null);

        assertThat(result).isEmpty();
        verify(repository).findEstado(2026, 3, null);
    }

    @Test
    void getEstado_calculaSemáforoRojo_cuandoConsumoSupera85Porciento() {
        Map<String, Object> row = Map.of(
                "equipo_id", 2,
                "numero_linea", "53001002",
                "ueb_id", 1,
                "ueb", "Nivel Central",
                "consumo_actual", new BigDecimal("2608.02"),
                "umbral", new BigDecimal("1200.00"),
                "umbral_ueb", 3
        );
        when(repository.findEstado(eq(2026), eq(11), isNull())).thenReturn(List.of(row));

        List<MonitoreoEstadoResponse> result = service.getEstado(2026, 11, null);

        assertThat(result).hasSize(1);
        MonitoreoEstadoResponse dto = result.get(0);
        assertThat(dto.equipoId()).isEqualTo(2);
        assertThat(dto.numeroLinea()).isEqualTo("53001002");
        assertThat(dto.ueb()).isEqualTo("Nivel Central");
        assertThat(dto.estado()).isEqualTo("rojo");
        assertThat(dto.porcentaje()).isGreaterThan(new BigDecimal("85"));
    }

    @Test
    void getEstado_calculaSemáforoAmarillo_entre70Y85() {
        Map<String, Object> row = Map.of(
                "equipo_id", 1,
                "numero_linea", "53001001",
                "ueb_id", 1,
                "ueb", "Nivel Central",
                "consumo_actual", new BigDecimal("900.00"),
                "umbral", new BigDecimal("1200.00"),
                "umbral_ueb", 3
        );
        when(repository.findEstado(eq(2026), eq(11), isNull())).thenReturn(List.of(row));

        MonitoreoEstadoResponse dto = service.getEstado(2026, 11, null).get(0);

        assertThat(dto.estado()).isEqualTo("amarillo");
    }

    @Test
    void getEstado_calculaSemáforoVerde_menorA70() {
        Map<String, Object> row = Map.of(
                "equipo_id", 1,
                "numero_linea", "53001001",
                "ueb_id", 1,
                "ueb", "Nivel Central",
                "consumo_actual", new BigDecimal("500.00"),
                "umbral", new BigDecimal("1200.00"),
                "umbral_ueb", 3
        );
        when(repository.findEstado(eq(2026), eq(11), isNull())).thenReturn(List.of(row));

        MonitoreoEstadoResponse dto = service.getEstado(2026, 11, null).get(0);

        assertThat(dto.estado()).isEqualTo("verde");
    }

    @Test
    void getEstado_sinPlan_devuelveVerdeYPorcentajeCero() {
        Map<String, Object> row = Map.of(
                "equipo_id", 1,
                "numero_linea", "53001001",
                "ueb_id", 1,
                "ueb", "Nivel Central",
                "consumo_actual", new BigDecimal("500.00"),
                "umbral", BigDecimal.ZERO,
                "umbral_ueb", 3
        );
        when(repository.findEstado(eq(2026), eq(11), isNull())).thenReturn(List.of(row));

        MonitoreoEstadoResponse dto = service.getEstado(2026, 11, null).get(0);

        assertThat(dto.estado()).isEqualTo("verde");
        assertThat(dto.porcentaje()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getEstado_mesInvalido_lanzaExcepcion() {
        assertThatThrownBy(() -> service.getEstado(2026, 13, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mes");
    }

    @Test
    void getEstado_filtraPorUeb_cuandoSeEspecifica() {
        when(repository.findEstado(2026, 11, 5)).thenReturn(List.of());

        service.getEstado(2026, 11, 5);

        verify(repository).findEstado(2026, 11, 5);
    }
}