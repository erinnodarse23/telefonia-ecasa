package cu.ecasa.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import cu.ecasa.api.dto.response.DashboardIndicadorResponse;
import cu.ecasa.api.repository.DashboardRepository;

@Service
public class DashboardService {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final DashboardRepository repository;

    public DashboardService(DashboardRepository repository) {
        this.repository = repository;
    }

    @Cacheable("dashboard-years")
    public List<Integer> availableYears() {
        List<Integer> years = new ArrayList<>(repository.findConsumptionYears());
        int current = java.time.Year.now().getValue();
        if (!years.contains(current)) years.add(current);
        if (!years.contains(current - 1)) years.add(current - 1);
        years.sort(java.util.Comparator.reverseOrder());
        return years;
    }

    public List<Map<String, Object>> monthlyTotals(Integer year, Integer uebId) {
        return repository.findMonthlyTotals(year, uebId).stream().map(row -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("mes", ((Number) row.get("mes")).intValue());
            result.put("total_voz", decimal(row.get("total_voz")));
            result.put("total_datos", decimal(row.get("total_datos")));
            result.put("total_sms", decimal(row.get("total_sms")));
            return result;
        }).toList();
    }

    public Map<String, Object> comparison(Integer year, Integer uebId) {
        List<Map<String, Object>> actual = new ArrayList<>();
        List<Map<String, Object>> previous = new ArrayList<>();
        for (Map<String, Object> row : repository.findAnnualComparison(year, uebId)) {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("mes", ((Number) row.get("mes")).intValue());
            value.put("total_voz", decimal(row.get("total_voz")));
            value.put("total_datos", decimal(row.get("total_datos")));
            value.put("total_sms", decimal(row.get("total_sms")));
            if (((Number) row.get("anio")).intValue() == year) actual.add(value); else previous.add(value);
        }
        return Map.of("anio_actual", year, "anio_anterior", year - 1, "actual", actual, "anterior", previous);
    }

    public List<Map<String, Object>> summary(Integer year, Integer uebId) {
        return repository.findSummary(year, uebId).stream().map(row -> {
            int services = ((Number) row.get("cantidad_servicios")).intValue();
            BigDecimal consumed = decimal(row.get("total_consumido"));
            BigDecimal monthlyPlan = decimal(row.get("plan_mensual"));
            BigDecimal average = services == 0 ? BigDecimal.ZERO : consumed.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
            BigDecimal annualPlan = decimal(repository.findAnnualPlan(((Number) row.get("ueb_id")).intValue(), year).get("monto"));
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("ueb_id", row.get("ueb_id")); result.put("codigo", row.get("codigo")); result.put("ueb_nombre", row.get("ueb_nombre"));
            result.put("cantidad_servicios", services); result.put("pct_lineas_con_datos", services == 0 ? 0 : Math.round(((Number) row.get("lineas_con_datos")).floatValue() / services * 100));
            result.put("total_voz", decimal(row.get("total_voz")).setScale(0, RoundingMode.HALF_UP)); result.put("total_datos", decimal(row.get("total_datos")).setScale(0, RoundingMode.HALF_UP));
            result.put("plan_anual", annualPlan); result.put("promedio_real_mensual", average); result.put("estimado_cierre", average.multiply(BigDecimal.valueOf(12)));
            result.put("diferencia", average.multiply(BigDecimal.valueOf(12)).subtract(annualPlan)); result.put("pct_sobregasto", monthlyPlan.signum() > 0 && average.compareTo(monthlyPlan) > 0 ? average.subtract(monthlyPlan).divide(monthlyPlan, 2, RoundingMode.HALF_UP).multiply(CIEN).setScale(0, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            result.put("total_consumido", consumed);
            result.put("lineas_con_datos", ((Number) row.get("lineas_con_datos")).intValue());
            result.put("plan_mensual", monthlyPlan);
            return result;
        }).toList();
    }

    /**
     * Indicadores consolidados por UEB para el año indicado.
     *
     * <p>Reutiliza {@link #summary(Integer, Integer)} para los datos base
     * (plan anual, promedios, pct_sobregasto) y añade:
     * <ul>
     *   <li>{@code consumos_mensuales}: 12 meses del año, con ceros en los
     *       meses sin datos</li>
     *   <li>{@code comparativa}: año actual vs anterior (12 meses cada uno,
     *       con ceros en los meses sin datos)</li>
     * </ul>
     *
     * <p>El agrupamiento por UEB se hace en memoria (2 queries totales,
     * no N+1): una para todos los consumos mensuales y otra para toda la
     * comparativa, ambas agrupadas por {@code ueb_id}.
     *
     * @param year  año a consultar
     * @param uebId ID de UEB (null para todas)
     * @return wrapper con {@code anio} y lista de indicadores
     */
    public Map<String, Object> calcularIndicadores(Integer year, Integer uebId) {
        List<Map<String, Object>> base = summary(year, uebId);

        Map<Integer, List<Map<String, Object>>> mensualesPorUeb = repository
                .findConsumosMensualesPorUeb(year, uebId).stream()
                .collect(Collectors.groupingBy(r -> ((Number) r.get("ueb_id")).intValue()));

        Map<Integer, List<Map<String, Object>>> comparativaPorUeb = repository
                .findComparativaPorUeb(year, uebId).stream()
                .collect(Collectors.groupingBy(r -> ((Number) r.get("ueb_id")).intValue()));

        List<DashboardIndicadorResponse> indicadores = base.stream().map(fila -> {
            Integer uebIdFila = ((Number) fila.get("ueb_id")).intValue();
            int services = ((Number) fila.get("cantidad_servicios")).intValue();
            int linesWithData = ((Number) fila.get("lineas_con_datos")).intValue();
            int porcentajeDatos = services == 0 ? 0 : Math.round((float) linesWithData / services * 100);

            List<DashboardIndicadorResponse.ConsumoMensual> consumos = rellenarMeses(
                    mensualesPorUeb.getOrDefault(uebIdFila, List.of()));

            DashboardIndicadorResponse.ComparativaAnual comparativa = construirComparativa(
                    year, comparativaPorUeb.getOrDefault(uebIdFila, List.of()));

            return new DashboardIndicadorResponse(
                    uebIdFila,
                    (String) fila.get("codigo"),
                    (String) fila.get("ueb_nombre"),
                    services,
                    linesWithData,
                    porcentajeDatos,
                    consumos,
                    decimal(fila.get("total_consumido")),
                    decimal(fila.get("plan_anual")),
                    decimal(fila.get("plan_mensual")),
                    decimal(fila.get("promedio_real_mensual")),
                    decimal(fila.get("estimado_cierre")),
                    decimal(fila.get("diferencia")),
                    decimal(fila.get("pct_sobregasto")),
                    decimal(fila.get("pct_sobregasto")),
                    comparativa
            );
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("anio", year);
        result.put("indicadores", indicadores);
        return result;
    }

    /**
     * Convierte las filas SQL de un UEB (solo meses con datos) en 12
     * {@link DashboardIndicadorResponse.ConsumoMensual} con ceros donde
     * falten meses.
     */
    private List<DashboardIndicadorResponse.ConsumoMensual> rellenarMeses(List<Map<String, Object>> filas) {
        Map<Integer, Map<String, Object>> porMes = filas.stream()
                .collect(Collectors.toMap(
                        r -> ((Number) r.get("mes")).intValue(),
                        r -> r,
                        (a, b) -> a));
        List<DashboardIndicadorResponse.ConsumoMensual> result = new ArrayList<>(12);
        for (int mes = 1; mes <= 12; mes++) {
            Map<String, Object> fila = porMes.get(mes);
            BigDecimal voz = fila == null ? BigDecimal.ZERO : decimal(fila.get("total_voz"));
            BigDecimal datos = fila == null ? BigDecimal.ZERO : decimal(fila.get("total_datos"));
            BigDecimal sms = fila == null ? BigDecimal.ZERO : decimal(fila.get("total_sms"));
            BigDecimal total = voz.add(datos).add(sms);
            result.add(new DashboardIndicadorResponse.ConsumoMensual(mes, voz, datos, sms, total));
        }
        return result;
    }

    /**
     * Construye la comparativa anual (año actual vs anterior) a partir de
     * las filas SQL de un UEB, rellenando con ceros los meses faltantes.
     */
    private DashboardIndicadorResponse.ComparativaAnual construirComparativa(
            Integer year, List<Map<String, Object>> filas) {
        List<DashboardIndicadorResponse.ConsumoMensual> actual = new ArrayList<>(12);
        List<DashboardIndicadorResponse.ConsumoMensual> anterior = new ArrayList<>(12);

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

        for (int mes = 1; mes <= 12; mes++) {
            actual.add(consumoDesdeFila(mes, actualPorMes.get(mes)));
            anterior.add(consumoDesdeFila(mes, anteriorPorMes.get(mes)));
        }

        return new DashboardIndicadorResponse.ComparativaAnual(year, year - 1, actual, anterior);
    }

    private DashboardIndicadorResponse.ConsumoMensual consumoDesdeFila(int mes, Map<String, Object> fila) {
        BigDecimal voz = fila == null ? BigDecimal.ZERO : decimal(fila.get("total_voz"));
        BigDecimal datos = fila == null ? BigDecimal.ZERO : decimal(fila.get("total_datos"));
        BigDecimal sms = fila == null ? BigDecimal.ZERO : decimal(fila.get("total_sms"));
        BigDecimal total = voz.add(datos).add(sms);
        return new DashboardIndicadorResponse.ConsumoMensual(mes, voz, datos, sms, total);
    }

    private BigDecimal decimal(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }
}