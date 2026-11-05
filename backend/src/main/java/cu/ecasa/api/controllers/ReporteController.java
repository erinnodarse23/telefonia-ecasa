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

import cu.ecasa.api.dto.response.TendenciasResponse;
import cu.ecasa.api.security.AuthenticatedUser;
import cu.ecasa.api.service.ReporteService;

@RestController
@RequestMapping("/api/reportes")
@PreAuthorize("hasAnyRole('GrupoComunicaciones', 'Administrador', 'JefeUnidad')")
public class ReporteController {

    private final ReporteService service;

    public ReporteController(ReporteService service) { this.service = service; }

    @GetMapping("/responsables")
    public List<Map<String, Object>> responsables(@RequestParam(required = false) Integer uebId, Authentication authentication) {
        return service.responsables(scopedUeb(authentication, uebId));
    }

    @GetMapping("/equipos/estado")
    public List<Map<String, Object>> equiposPorEstado(@RequestParam(required = false) String estado, Authentication authentication) {
        return service.equiposPorEstado(estado, scopedUeb(authentication, null));
    }

    /**
     * Tendencias del año: consumos mensuales, ranking top/bottom de equipos
     * y comparativa con el año anterior.
     *
     * @param anio año a consultar (default: año actual)
     * @param uebId ID de UEB (opcional; JefeUnidad siempre ve la suya)
     * @param auth usuario autenticado
     */
    @GetMapping("/tendencias")
    public ResponseEntity<?> tendencias(
            @RequestParam(required = false) Integer anio,
            @RequestParam(name = "uebId", required = false) Integer uebId,
            Authentication auth) {
        int anioFinal = anio != null ? anio : java.time.Year.now().getValue();
        TendenciasResponse data = service.getTendencias(anioFinal, scopedUeb(auth, uebId));
        return ResponseEntity.ok(data);
    }

    private Integer scopedUeb(Authentication authentication, Integer requested) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return "JefeUnidad".equals(user.rol()) ? user.uebId() : requested;
    }
}