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

import cu.ecasa.api.dto.request.AsignacionRequest;
import cu.ecasa.api.dto.request.FinalizarAsignacionRequest;
import cu.ecasa.api.dto.response.AsignacionResponse;
import cu.ecasa.api.service.AsignacionService;

import jakarta.validation.Valid;

/**
 * Controlador de asignaciones. Los DTOs tipados permiten que Swagger
 * documente automáticamente los esquemas.
 */
@RestController
@RequestMapping("/api/asignaciones")
public class AsignacionController {

    private final AsignacionService service;

    public AsignacionController(AsignacionService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('Administrador', 'GrupoComunicaciones', 'JefeUnidad')")
    public List<AsignacionResponse> findAll(@RequestParam(required = false) String estado) {
        return service.findAll(estado);
    }

    @GetMapping("/equipo/{equipoId}")
    @PreAuthorize("hasAnyRole('Administrador', 'GrupoComunicaciones', 'JefeUnidad')")
    public List<AsignacionResponse> findByEquipo(@PathVariable Integer equipoId,
                                                 @RequestParam(required = false) String estado) {
        return service.findByEquipo(equipoId, estado);
    }

    @GetMapping("/empleado/{empleadoId}")
    @PreAuthorize("hasAnyRole('Administrador', 'GrupoComunicaciones', 'JefeUnidad')")
    public List<AsignacionResponse> findByEmpleado(@PathVariable Integer empleadoId,
                                                   @RequestParam(required = false) String estado) {
        return service.findByEmpleado(empleadoId, estado);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('Administrador', 'GrupoComunicaciones', 'JefeUnidad')")
    public AsignacionResponse findById(@PathVariable Integer id) {
        return service.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('Administrador', 'GrupoComunicaciones')")
    public ResponseEntity<AsignacionResponse> create(@Valid @RequestBody AsignacionRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }

    @PutMapping("/{id}/finalizar")
    @PreAuthorize("hasAnyRole('Administrador', 'GrupoComunicaciones')")
    public AsignacionResponse finalizar(@PathVariable Integer id,
                                        @Valid @RequestBody FinalizarAsignacionRequest request) {
        return service.finalizar(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}