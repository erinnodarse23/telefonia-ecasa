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

import cu.ecasa.api.dto.request.UsuarioRequest;
import cu.ecasa.api.dto.request.UsuarioUpdateRequest;
import cu.ecasa.api.dto.response.UsuarioResponse;
import cu.ecasa.api.service.UsuarioService;

import jakarta.validation.Valid;

/**
 * Controlador de usuarios del sistema.
 *
 * <p>Todos los endpoints requieren rol {@code Administrador}. Los DTOs
 * tipados permiten que Swagger documente automáticamente los esquemas de
 * request y response.
 */
@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('Administrador')")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<UsuarioResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public UsuarioResponse findById(@PathVariable Integer id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> create(@Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }

    @PutMapping("/{id}")
    public UsuarioResponse update(@PathVariable Integer id,
                                  @Valid @RequestBody UsuarioUpdateRequest request) {
        return service.update(id, request);
    }

    @PutMapping("/{id}/activar")
    public UsuarioResponse activate(@PathVariable Integer id) {
        return service.setActivo(id, true);
    }

    @PutMapping("/{id}/desactivar")
    public UsuarioResponse deactivate(@PathVariable Integer id) {
        return service.setActivo(id, false);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}