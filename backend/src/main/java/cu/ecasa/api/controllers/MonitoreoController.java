package cu.ecasa.api.controllers;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cu.ecasa.api.dto.response.MonitoreoEstadoResponse;
import cu.ecasa.api.security.AuthenticatedUser;
import cu.ecasa.api.service.MonitoreoService;

/**
 * Endpoint de monitoreo de consumo por equipo.
 *
 * <p>Devuelve el estado de cada equipo activo con su semáforo
 * verde/amarillo/rojo según el porcentaje de sobregasto respecto al umbral
 * de su UEB.
 *
 * <p>El frontend {@code Monitoreo.jsx} refresca cada 30 segundos.
 */
@RestController
@RequestMapping("/api/monitoreo")
@PreAuthorize("hasAnyRole('GrupoComunicaciones', 'Administrador', 'JefeUnidad')")
public class MonitoreoController {

    private final MonitoreoService service;

    public MonitoreoController(MonitoreoService service) {
        this.service = service;
    }

    /**
     * Estado de monitoreo por equipo.
     *
     * @param anio  año (opcional, default: año actual)
     * @param mes   mes 1-12 (opcional, default: mes actual si el año es el actual, si no 12)
     * @param uebId ID de UEB (opcional, solo Administrador/GrupoComunicaciones)
     * @param auth  usuario autenticado (para el filtro por rol)
     * @return lista de estados por equipo
     */
    @GetMapping("/estado")
    public List<MonitoreoEstadoResponse> estado(
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes,
            @RequestParam(name = "uebId", required = false) Integer uebId,
            Authentication auth) {
        return service.getEstado(anio, mes, scopedUeb(auth, uebId));
    }

    /**
     * Resuelve el UEB a filtrar según el rol:
     * <ul>
     *   <li>{@code JefeUnidad} → siempre su propia UEB (ignora el query param)</li>
     *   <li>Otros roles → el {@code uebId} del query param, o {@code null} para ver todas</li>
     * </ul>
     */
    private Integer scopedUeb(Authentication authentication, Integer requestedUebId) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return "JefeUnidad".equals(user.rol()) ? user.uebId() : requestedUebId;
    }
}