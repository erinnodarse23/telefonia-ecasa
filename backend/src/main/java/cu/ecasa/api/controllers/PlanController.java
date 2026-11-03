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
import org.springframework.web.bind.annotation.RestController;

import cu.ecasa.api.dto.request.FinalizePlanRequest;
import cu.ecasa.api.dto.request.PlanRequest;
import cu.ecasa.api.dto.request.PlanUpdateRequest;
import cu.ecasa.api.dto.response.PlanResponse;
import cu.ecasa.api.service.PlanService;

import jakarta.validation.Valid;

/**
 * Controlador de planes. Los DTOs tipados permiten que Swagger documente
 * automáticamente los esquemas.
 */
@RestController
@RequestMapping("/api/planes")
public class PlanController {

    private final PlanService service;

    public PlanController(PlanService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('Administrador', 'GrupoComunicaciones', 'JefeUnidad')")
    public List<PlanResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/equipo/{equipoId}")
    @PreAuthorize("hasAnyRole('Administrador', 'GrupoComunicaciones', 'JefeUnidad')")
    public List<PlanResponse> findByEquipo(@PathVariable Integer equipoId) {
        return service.findByEquipo(equipoId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('Administrador', 'GrupoComunicaciones', 'JefeUnidad')")
    public PlanResponse findById(@PathVariable Integer id) {
        return service.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('Administrador', 'GrupoComunicaciones')")
    public ResponseEntity<PlanResponse> create(@Valid @RequestBody PlanRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('Administrador', 'GrupoComunicaciones')")
    public PlanResponse update(@PathVariable Integer id,
                               @Valid @RequestBody PlanUpdateRequest request) {
        return service.update(id, request);
    }

    @PutMapping("/{id}/finalizar")
    @PreAuthorize("hasAnyRole('Administrador', 'GrupoComunicaciones')")
    public PlanResponse finalizePlan(@PathVariable Integer id,
                                     @Valid @RequestBody FinalizePlanRequest request) {
        return service.finalizePlan(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}