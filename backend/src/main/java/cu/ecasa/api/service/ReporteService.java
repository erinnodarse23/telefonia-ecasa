package cu.ecasa.api.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import cu.ecasa.api.dto.response.TendenciasResponse;
import cu.ecasa.api.repository.ReporteRepository;

@Service
public class ReporteService {

    private static final int RANKING_LIMIT = 10;

    private final ReporteRepository repository;

    public ReporteService(ReporteRepository repository) {
        this.repository = repository;
    }

    public List<Map<String, Object>> responsables(Integer uebId) {
        return repository.responsables(uebId);
    }

    public List<Map<String, Object>> equiposPorEstado(String estado, Integer uebId) {
        if (estado != null && !java.util.Set.of("nuevo", "bueno", "dañado", "de baja").contains(estado)) {
            throw new IllegalArgumentException("Estado inválido");
        }
        return repository.equiposPorEstado(estado, uebId);
    }

    /**
     * Tendencias del año: consumos mensuales, ranking top/bottom de equipos
     * y comparativa con el año anterior.
     *
     * @param year  año a consultar
     * @param uebId ID de UEB (null = todas)
     * @return {@link TendenciasResponse} con todos los bloques
     */
    public TendenciasResponse getTendencias(Integer year, Integer uebId) {
        List<TendenciasResponse.ConsumoMensual> consumos =
                rellenarMeses(repository.findConsumosMensuales(year, uebId));

        List<TendenciasResponse.RankingItem> rankingTop =
                mapearRanking(repository.findRankingTop(year, uebId, RANKING_LIMIT));
        List<TendenciasResponse.RankingItem> rankingBottom =
                mapearRanking(repository.findRankingBottom(year, uebId, RANKING_LIMIT));

        TendenciasResponse.ComparativaAnual comparativa =
                construirComparativa(year, repository.findComparativaAnual(year, uebId));

        return new TendenciasResponse(year, consumos, rankingTop, rankingBottom, comparativa);
    }

    private List<TendenciasResponse.ConsumoMensual> rellenarMeses(List<Map<String, Object>> filas) {
        Map<Integer, Map<String, Object>> porMes = filas.stream()
                .collect(Collectors.toMap(
                        r -> ((Number) r.get("mes")).intValue(),
                        r -> r,
                        (a, b) -> a));
        List<TendenciasResponse.ConsumoMensual> result = new ArrayList<>(12);
        for (int mes = 1; mes <= 12; mes++) {
            result.add(consumoDesdeFila(mes, porMes.get(mes)));
        }
        return result;
    }

    private TendenciasResponse.ConsumoMensual consumoDesdeFila(int mes, Map<String, Object> fila) {
        BigDecimal voz = fila == null ? BigDecimal.ZERO : decimal(fila.get("total_voz"));
        BigDecimal datos = fila == null ? BigDecimal.ZERO : decimal(fila.get("total_datos"));
        BigDecimal sms = fila == null ? BigDecimal.ZERO : decimal(fila.get("total_sms"));
        BigDecimal total = voz.add(datos).add(sms);
        return new TendenciasResponse.ConsumoMensual(mes, voz, datos, sms, total);
    }

    private List<TendenciasResponse.RankingItem> mapearRanking(List<Map<String, Object>> filas) {
        return filas.stream().map(fila -> {
            BigDecimal voz = decimal(fila.get("total_voz"));
            BigDecimal datos = decimal(fila.get("total_datos"));
            BigDecimal sms = decimal(fila.get("total_sms"));
            BigDecimal total = decimal(fila.get("total"));
            return new TendenciasResponse.RankingItem(
                    ((Number) fila.get("equipo_id")).intValue(),
                    (String) fila.get("numero_linea"),
                    fila.get("ueb_id") == null ? null : ((Number) fila.get("ueb_id")).intValue(),
                    (String) fila.get("ueb"),
                    voz, datos, sms, total
            );
        }).toList();
    }

    private TendenciasResponse.ComparativaAnual construirComparativa(
            Integer year, List<Map<String, Object>> filas) {
        Map<Integer, Map<String, Object>> actualPorMes = filas.stream()
                .filter(r -> ((Number) r.get("anio")).intValue() == year)
                .collect(Collectors.toMap(
                        r -> ((Number) r.get("mes")).intValue(),
                        r -> r,
                        (a, b) -> a));
        Map<Integer, Map<String, Object>> anteriorPorMes = filas.stream()
                .filter(r -> ((Number) r.get("anio")).intValue() == year - 1)
                .collect(Collectors.toMap(
                        r -> ((Number) r.get("mes")).intValue(),
                        r -> r,
                        (a, b) -> a));

        List<TendenciasResponse.ConsumoMensual> actual = new ArrayList<>(12);
        List<TendenciasResponse.ConsumoMensual> anterior = new ArrayList<>(12);
        for (int mes = 1; mes <= 12; mes++) {
            actual.add(consumoDesdeFila(mes, actualPorMes.get(mes)));
            anterior.add(consumoDesdeFila(mes, anteriorPorMes.get(mes)));
        }
        return new TendenciasResponse.ComparativaAnual(year, year - 1, actual, anterior);
    }

    private BigDecimal decimal(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }
}