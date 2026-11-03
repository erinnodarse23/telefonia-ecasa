package cu.ecasa.api.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cu.ecasa.api.dto.request.EquipoRequest;
import cu.ecasa.api.dto.request.EquipoUpdateRequest;
import cu.ecasa.api.dto.response.EquipoResponse;
import cu.ecasa.api.service.EquipoService;

import jakarta.validation.Valid;

/**
 * Controlador de equipos. Los DTOs tipados permiten que Swagger documente
 * automáticamente los esquemas.
 */
@RestController
@RequestMapping("/api/equipos")
public class EquipoController {

    private final EquipoService service;

    public EquipoController(EquipoService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('Administrador', 'GrupoComunicaciones')")
    public List<EquipoResponse> findAll(@RequestParam(required = false) Boolean activo,
                                        @RequestParam(required = false) String estado) {
        return service.findAll(activo, estado);
    }

    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('Administrador', 'GrupoComunicaciones')")
    public EquipoResponse findByNumero(@RequestParam String numero) {
        return service.findByNumero(numero.trim());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('Administrador', 'GrupoComunicaciones')")
    public EquipoResponse findById(@PathVariable Integer id) {
        return service.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<EquipoResponse> create(@Valid @RequestBody EquipoRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('Administrador')")
    public EquipoResponse update(@PathVariable Integer id,
                                 @Valid @RequestBody EquipoUpdateRequest request) {
        return service.update(id, request);
    }

    @PutMapping("/{id}/activar")
    @PreAuthorize("hasAnyRole('Administrador', 'GrupoComunicaciones')")
    public EquipoResponse activate(@PathVariable Integer id) {
        return service.setActivo(id, true);
    }

    @PutMapping("/{id}/desactivar")
    @PreAuthorize("hasAnyRole('Administrador', 'GrupoComunicaciones')")
    public EquipoResponse deactivate(@PathVariable Integer id) {
        return service.setActivo(id, false);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}