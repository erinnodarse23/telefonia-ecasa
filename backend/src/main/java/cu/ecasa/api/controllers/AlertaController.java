package cu.ecasa.api.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cu.ecasa.api.security.AuthenticatedUser;
import cu.ecasa.api.service.AlertaService;

@RestController
@RequestMapping("/api/alertas")
@PreAuthorize("hasAnyRole('Administrador', 'GrupoComunicaciones', 'JefeUnidad')")
public class AlertaController {

    private final AlertaService service;

    public AlertaController(AlertaService service) { this.service = service; }

    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) String tipo, @RequestParam(required = false) Boolean leida,
            @RequestParam(required = false) Boolean solo_activas, @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "50") int limite, Authentication authentication) {
        return service.list(tipo, leida, solo_activas, scopedUeb(authentication), pagina, limite);
    }

    @GetMapping("/contar")
    public Map<String, Long> count(Authentication authentication) { return Map.of("total", service.unreadCount(scopedUeb(authentication))); }

    @GetMapping("/recientes")
    public List<Map<String, Object>> recent(@RequestParam(defaultValue = "5") int limite, Authentication authentication) {
        return service.recent(limite, scopedUeb(authentication));
    }

    @PutMapping("/{id}/leer")
    public Map<String, Object> markRead(@PathVariable Integer id) { return service.markRead(id); }

    @PutMapping("/leer-todas")
    public Map<String, Object> markAllRead(Authentication authentication) {
        int count = service.markAllRead(scopedUeb(authentication));
        return Map.of("mensaje", count + " alerta(s) marcada(s) como leída(s)");
    }

    @PutMapping("/{id}/descartar")
    @PreAuthorize("hasAnyRole('Administrador', 'GrupoComunicaciones')")
    public Map<String, Object> discard(@PathVariable Integer id) { return service.discard(id); }

    @PutMapping("/archivar-todas")
    public Map<String, Object> archiveAll(Authentication authentication) {
        int count = service.archiveAll(scopedUeb(authentication));
        return Map.of("mensaje", count + " alerta(s) archivada(s)", "archivadas", count);
    }

    private Integer scopedUeb(Authentication authentication) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return "JefeUnidad".equals(user.rol()) ? user.uebId() : null;
    }
}