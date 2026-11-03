package cu.ecasa.api.controllers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cu.ecasa.api.service.ConfiguracionService;

@RestController
@RequestMapping("/api/configuracion")
@PreAuthorize("hasRole('Administrador')")
public class ConfiguracionController {

    private final ConfiguracionService service;

    public ConfiguracionController(ConfiguracionService service) { this.service = service; }

    @GetMapping("/planes")
    public List<Map<String, Object>> annualPlans(@RequestParam Integer anio) { return service.annualPlans(anio); }

    @GetMapping("/planes/{uebId}")
    public Map<String, Object> annualPlan(@PathVariable Integer uebId, @RequestParam Integer anio) { return service.annualPlan(uebId, anio); }

    @PostMapping("/planes")
    public Map<String, Object> saveAnnualPlan(@RequestBody AnnualPlanRequest request) {
        return Map.of("message", "Plan guardado correctamente", "plan", service.saveAnnualPlan(request.uebId(), request.anio(), request.monto()));
    }

    @GetMapping("/datos-anteriores")
    public Map<String, Object> previousData(@RequestParam Integer anio) { return Map.of("anio", anio, "datos", service.jsonConfig("DATOS_ANTERIORES", anio)); }

    @PostMapping("/datos-anteriores")
    public Map<String, String> savePreviousData(@RequestBody JsonConfigRequest request) {
        service.saveJsonConfig("DATOS_ANTERIORES", request.anio(), request.datos());
        return Map.of("message", "Datos anteriores guardados correctamente");
    }

    @GetMapping("/umbral")
    public Object threshold() { return service.jsonConfig("UMBRAL", null); }

    @PostMapping("/umbral")
    public Map<String, String> saveThreshold(@RequestBody JsonNode value) {
        service.saveJsonConfig("UMBRAL", null, value.has("valor") ? value.get("valor") : value);
        return Map.of("message", "Umbral guardado correctamente");
    }

    @GetMapping("/umbrales")
    public List<Map<String, Object>> thresholds() { return service.thresholds(); }

    @PostMapping("/umbrales")
    public Map<String, String> saveThresholds(@RequestBody ThresholdsRequest request) {
        service.saveThresholds(request.umbrales());
        return Map.of("message", "Umbrales guardados correctamente");
    }

    public record AnnualPlanRequest(Integer uebId, Integer anio, BigDecimal monto) { }
    public record JsonConfigRequest(Integer anio, JsonNode datos) { }
    public record ThresholdsRequest(List<JsonNode> umbrales) { }
}