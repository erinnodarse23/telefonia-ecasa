package cu.ecasa.api.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cu.ecasa.api.security.AuthenticatedUser;
import cu.ecasa.api.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasAnyRole('GrupoComunicaciones', 'Administrador', 'JefeUnidad')")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping({"/anios", "/anios-disponibles"})
    public Map<String, Object> years() {
        return Map.of("anios", service.availableYears());
    }

    @GetMapping("/totales")
    public ResponseEntity<?> monthly(@RequestParam Integer anio, Authentication authentication) {
        List<Map<String, Object>> data = service.monthlyTotals(anio, scopedUeb(authentication));
        if (data.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "No hay datos de consumo para el año especificado"));
        return ResponseEntity.ok(Map.of("anio", anio, "meses", data));
    }

    @GetMapping("/comparativa")
    public ResponseEntity<?> comparison(@RequestParam Integer anio, Authentication authentication) {
        Map<String, Object> data = service.comparison(anio, scopedUeb(authentication));
        if (((List<?>) data.get("actual")).isEmpty() && ((List<?>) data.get("anterior")).isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "No hay datos de consumo para los años especificados"));
        return ResponseEntity.ok(data);
    }

    @GetMapping("/resumen")
    public ResponseEntity<?> summary(@RequestParam Integer anio, Authentication authentication) {
        List<Map<String, Object>> data = service.summary(anio, scopedUeb(authentication));
        if (data.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "No hay datos para el año especificado"));
        return ResponseEntity.ok(Map.of("anio", anio, "uebs", data));
    }

    private Integer scopedUeb(Authentication authentication) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return "JefeUnidad".equals(user.rol()) ? user.uebId() : null;
    }
}