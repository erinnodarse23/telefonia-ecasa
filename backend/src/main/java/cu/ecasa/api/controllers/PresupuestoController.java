package cu.ecasa.api.controllers;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cu.ecasa.api.dto.request.PresupuestoUpdateRequest;
import cu.ecasa.api.dto.response.PresupuestoResponse;
import cu.ecasa.api.security.AuthenticatedUser;
import cu.ecasa.api.service.PresupuestoService;

import jakarta.validation.Valid;

/**
 * Endpoints del presupuesto anual por UEB.
 *
 * <ul>
 *   <li>{@code GET /api/presupuesto} — consulta (cualquier rol autenticado,
 *       con scoped UEB para JefeUnidad)</li>
 *   <li>{@code POST /api/presupuesto} — actualiza (solo Administrador)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/presupuesto")
@PreAuthorize("hasAnyRole('GrupoComunicaciones', 'Administrador', 'JefeUnidad')")
public class PresupuestoController {

    private final PresupuestoService service;

    public PresupuestoController(PresupuestoService service) {
        this.service = service;
    }

    /**
     * Consulta el presupuesto anual por UEB.
     *
     * @param anio año a consultar (default: año actual)
     * @param uebId ID de UEB (opcional; JefeUnidad siempre ve la suya)
     * @param auth usuario autenticado (para el scoped por rol)
     */
    @GetMapping
    public List<PresupuestoResponse> get(
            @RequestParam(required = false) Integer anio,
            @RequestParam(name = "uebId", required = false) Integer uebId,
            Authentication auth) {
        int anioFinal = anio != null ? anio : java.time.Year.now().getValue();
        return service.getPresupuesto(anioFinal, scopedUeb(auth, uebId));
    }

    /**
     * Actualiza el presupuesto anual de una UEB.
     * <p>Solo {@code Administrador}. Tras guardar, reevalúa alertas de
     * presupuesto (70%, 85%, 100%).
     */
    @PostMapping
    @PreAuthorize("hasRole('Administrador')")
    public PresupuestoResponse update(@Valid @RequestBody PresupuestoUpdateRequest body) {
        return service.setAsignado(body.anio(), body.uebId(), body.asignado());
    }

    /**
     * Resuelve el UEB a filtrar según el rol:
     * <ul>
     *   <li>{@code JefeUnidad} → siempre su propia UEB</li>
     *   <li>Otros roles → el {@code uebId} del query param, o {@code null}</li>
     * </ul>
     */
    private Integer scopedUeb(Authentication authentication, Integer requestedUebId) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return "JefeUnidad".equals(user.rol()) ? user.uebId() : requestedUebId;
    }
}