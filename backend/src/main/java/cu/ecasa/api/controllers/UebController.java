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

import cu.ecasa.api.dto.request.UebRequest;
import cu.ecasa.api.dto.response.UebResponse;
import cu.ecasa.api.service.UebService;

import jakarta.validation.Valid;

/**
 * Controlador de UEBs. Los DTOs tipados permiten que Swagger documente
 * automáticamente los esquemas de request y response.
 */
@RestController
@RequestMapping("/api/ueb")
public class UebController {

    private final UebService service;

    public UebController(UebService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('Administrador', 'GrupoComunicaciones')")
    public List<UebResponse> findAll(@RequestParam(required = false) Boolean activo) {
        return service.findAll(activo);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('Administrador', 'GrupoComunicaciones')")
    public UebResponse findById(@PathVariable Integer id) {
        return service.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<UebResponse> create(@Valid @RequestBody UebRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('Administrador')")
    public UebResponse update(@PathVariable Integer id, @Valid @RequestBody UebRequest request) {
        return service.update(id, request);
    }

    @PutMapping("/{id}/activar")
    @PreAuthorize("hasAnyRole('Administrador', 'GrupoComunicaciones')")
    public UebResponse activate(@PathVariable Integer id) {
        return service.setActivo(id, true);
    }

    @PutMapping("/{id}/desactivar")
    @PreAuthorize("hasAnyRole('Administrador', 'GrupoComunicaciones')")
    public UebResponse deactivate(@PathVariable Integer id) {
        return service.setActivo(id, false);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}