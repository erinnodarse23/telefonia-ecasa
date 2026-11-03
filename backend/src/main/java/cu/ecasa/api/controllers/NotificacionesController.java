package cu.ecasa.api.controllers;

import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cu.ecasa.api.security.AuthenticatedUser;
import cu.ecasa.api.service.AlertaService;

@RestController
@RequestMapping("/api/notificaciones")
@PreAuthorize("hasAnyRole('Administrador', 'GrupoComunicaciones', 'JefeUnidad')")
public class NotificacionesController {

    private final AlertaService service;

    public NotificacionesController(AlertaService service) { this.service = service; }

    @PostMapping("/limpiar")
    public Map<String, Object> clear(Authentication authentication) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        int count = service.markAllRead("JefeUnidad".equals(user.rol()) ? user.uebId() : null);
        return Map.of("mensaje", count + " notificación(es) marcada(s) como leída(s)", "limpiadas", count);
    }
}