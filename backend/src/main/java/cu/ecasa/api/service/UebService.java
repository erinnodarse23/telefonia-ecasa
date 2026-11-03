package cu.ecasa.api.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cu.ecasa.api.dto.request.UebRequest;
import cu.ecasa.api.dto.response.UebResponse;
import cu.ecasa.api.mapper.UebMapper;
import cu.ecasa.api.repository.UebRepository;
import cu.ecasa.api.exceptions.ResourceNotFoundException;
import cu.ecasa.api.config.XssSanitizer;

/**
 * Servicio de gestión de UEBs con DTOs, auditoría y sanitización XSS.
 *
 * @see UebRepository
 * @see UebMapper
 * @see TrazaService
 * @see XssSanitizer
 */
@Service
public class UebService {

    private final UebRepository repository;
    private final TrazaService trazaService;

    public UebService(UebRepository repository, TrazaService trazaService) {
        this.repository = repository;
        this.trazaService = trazaService;
    }

    public List<UebResponse> findAll(Boolean activo) {
        return UebMapper.toResponseList(repository.findAll(activo));
    }

    public UebResponse findById(Integer id) {
        return repository.findById(id)
                .map(UebMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("UEB no encontrada"));
    }

    @Transactional
    public UebResponse create(UebRequest request) {
        String normalizedCodigo = validateCodigo(request.codigo());
        String safeNombre = (String) XssSanitizer.sanitizeValue(request.nombre());
        String safeDescripcion = (String) XssSanitizer.sanitizeValue(request.descripcion());
        String normalizedNombre = validateNombre(safeNombre);

        if (repository.findByCodigo(normalizedCodigo).isPresent()) {
            throw new IllegalArgumentException("El código de UEB ya existe");
        }

        Map<String, Object> result = repository.create(
                normalizedCodigo, normalizedNombre, emptyToNull(safeDescripcion));
        trazaService.registrarAuto("CREATE", "UEB",
                ((Number) result.get("id")).intValue(),
                "Alta de UEB '" + normalizedCodigo + " - " + normalizedNombre + "'");
        return UebMapper.toResponse(result);
    }

    @Transactional
    public UebResponse update(Integer id, UebRequest request) {
        UebResponse existente = findById(id);

        String normalizedCodigo = request.codigo() == null ? null : validateCodigo(request.codigo());
        String safeNombre = (String) XssSanitizer.sanitizeValue(request.nombre());
        String safeDescripcion = (String) XssSanitizer.sanitizeValue(request.descripcion());
        String normalizedNombre = safeNombre == null ? null : validateNombre(safeNombre);

        if (normalizedCodigo != null) {
            repository.findByCodigo(normalizedCodigo).ifPresent(existing -> {
                if (!existing.get("id").equals(id)) {
                    throw new IllegalArgumentException("El código de UEB ya está asignado a otra UEB");
                }
            });
        }

        Map<String, Object> result = repository.update(
                id, normalizedCodigo, normalizedNombre, safeDescripcion);
        trazaService.registrarAuto("UPDATE", "UEB", id,
                "Actualización de UEB '" + existente.codigo() + "'");
        return UebMapper.toResponse(result);
    }

    @Transactional
    public UebResponse setActivo(Integer id, boolean activo) {
        findById(id);
        Map<String, Object> result = repository.setActivo(id, activo);
        trazaService.registrarAuto("UPDATE", "UEB", id,
                activo ? "Activación de UEB" : "Desactivación de UEB");
        return UebMapper.toResponse(result);
    }

    @Transactional
    public void delete(Integer id) {
        UebResponse existente = findById(id);
        if (repository.countActiveEmployees(id) > 0) {
            throw new IllegalArgumentException(
                    "No se puede eliminar la UEB porque tiene empleados activos asociados. Desactívelos primero.");
        }
        repository.delete(id);
        trazaService.registrarAuto("DELETE", "UEB", id,
                "Eliminación de UEB '" + existente.codigo() + "'");
    }

    private String validateCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) throw new IllegalArgumentException("El código es obligatorio");
        String value = codigo.trim();
        if (value.length() < 2 || value.length() > 10 || !value.matches("[a-zA-Z0-9]+")) {
            throw new IllegalArgumentException("El código debe tener entre 2 y 10 caracteres alfanuméricos");
        }
        return value;
    }

    private String validateNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("El nombre es obligatorio");
        String value = nombre.trim();
        if (value.length() > 100) throw new IllegalArgumentException("El nombre no puede exceder 100 caracteres");
        return value;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}