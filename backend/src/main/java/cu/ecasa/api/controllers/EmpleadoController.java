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

import cu.ecasa.api.dto.request.EmpleadoRequest;
import cu.ecasa.api.dto.request.EmpleadoUpdateRequest;
import cu.ecasa.api.dto.response.EmpleadoResponse;
import cu.ecasa.api.service.EmpleadoService;

import jakarta.validation.Valid;

/**
 * Controlador de empleados. Los DTOs tipados permiten que Swagger documente
 * automáticamente los esquemas.
 */
@RestController
@RequestMapping("/api/empleados")
public class EmpleadoController {

    private final EmpleadoService service;

    public EmpleadoController(EmpleadoService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('Administrador', 'GrupoComunicaciones', 'JefeUnidad')")
    public List<EmpleadoResponse> findAll(@RequestParam(required = false) Boolean activo) {
        return service.findAll(activo);
    }

    @GetMapping("/ueb/{uebId}")
    @PreAuthorize("hasAnyRole('Administrador', 'GrupoComunicaciones', 'JefeUnidad')")
    public List<EmpleadoResponse> findByUeb(@PathVariable Integer uebId,
                                            @RequestParam(required = false) Boolean activo) {
        return service.findByUeb(uebId, activo);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('Administrador', 'GrupoComunicaciones', 'JefeUnidad')")
    public EmpleadoResponse findById(@PathVariable Integer id) {
        return service.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('Administrador', 'GrupoComunicaciones')")
    public ResponseEntity<EmpleadoResponse> create(@Valid @RequestBody EmpleadoRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('Administrador', 'GrupoComunicaciones')")
    public EmpleadoResponse update(@PathVariable Integer id,
                                   @Valid @RequestBody EmpleadoUpdateRequest request) {
        return service.update(id, request);
    }

    @PutMapping("/{id}/activar")
    @PreAuthorize("hasAnyRole('Administrador', 'GrupoComunicaciones')")
    public EmpleadoResponse activate(@PathVariable Integer id) {
        return service.setActivo(id, true);
    }

    @PutMapping("/{id}/desactivar")
    @PreAuthorize("hasAnyRole('Administrador', 'GrupoComunicaciones')")
    public EmpleadoResponse deactivate(@PathVariable Integer id) {
        return service.setActivo(id, false);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}