package cu.ecasa.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import cu.ecasa.api.repository.DashboardRepository;

@Service
public class DashboardService {

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
            result.put("diferencia", average.multiply(BigDecimal.valueOf(12)).subtract(annualPlan)); result.put("pct_sobregasto", monthlyPlan.signum() > 0 && average.compareTo(monthlyPlan) > 0 ? average.subtract(monthlyPlan).divide(monthlyPlan, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP) : 0);
            result.put("total_consumido", consumed); return result;
        }).toList();
    }

    private BigDecimal decimal(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }
}