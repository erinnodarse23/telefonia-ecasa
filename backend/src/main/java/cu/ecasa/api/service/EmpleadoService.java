package cu.ecasa.api.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cu.ecasa.api.dto.request.EmpleadoRequest;
import cu.ecasa.api.dto.request.EmpleadoUpdateRequest;
import cu.ecasa.api.dto.response.EmpleadoResponse;
import cu.ecasa.api.mapper.EmpleadoMapper;
import cu.ecasa.api.repository.EmpleadoRepository;
import cu.ecasa.api.repository.UebRepository;
import cu.ecasa.api.exceptions.ResourceNotFoundException;
import cu.ecasa.api.config.XssSanitizer;

/**
 * Servicio de gestión de empleados con DTOs, auditoría y sanitización XSS.
 *
 * @see EmpleadoRepository
 * @see EmpleadoMapper
 * @see TrazaService
 * @see XssSanitizer
 */
@Service
public class EmpleadoService {

    private final EmpleadoRepository repository;
    private final UebRepository uebRepository;
    private final TrazaService trazaService;

    public EmpleadoService(EmpleadoRepository repository, UebRepository uebRepository,
                           TrazaService trazaService) {
        this.repository = repository;
        this.uebRepository = uebRepository;
        this.trazaService = trazaService;
    }

    public List<EmpleadoResponse> findAll(Boolean activo) {
        return EmpleadoMapper.toResponseList(repository.findAll(activo));
    }

    public List<EmpleadoResponse> findByUeb(Integer uebId, Boolean activo) {
        validateUeb(uebId);
        return EmpleadoMapper.toResponseList(repository.findByUeb(uebId, activo));
    }

    public EmpleadoResponse findById(Integer id) {
        return repository.findById(id)
                .map(EmpleadoMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado"));
    }

    @Transactional
    public EmpleadoResponse create(EmpleadoRequest request) {
        String safeNombre = (String) XssSanitizer.sanitizeValue(request.nombre());
        String safeCargo = (String) XssSanitizer.sanitizeValue(request.cargo());
        String safeDepartamento = (String) XssSanitizer.sanitizeValue(request.departamento());

        validateNombre(safeNombre);
        validateTelefono(request.telefono());
        validateUeb(request.uebId());

        Map<String, Object> result = repository.create(
                safeNombre.trim(),
                safeCargo,
                safeDepartamento,
                emptyToNull(request.telefono()),
                request.uebId());

        trazaService.registrarAuto("CREATE", "EMPLEADO",
                ((Number) result.get("id")).intValue(),
                "Alta de empleado '" + safeNombre.trim() + "'");
        return EmpleadoMapper.toResponse(result);
    }

    @Transactional
    public EmpleadoResponse update(Integer id, EmpleadoUpdateRequest request) {
        EmpleadoResponse existente = findById(id);

        String safeNombre = (String) XssSanitizer.sanitizeValue(request.nombre());
        String safeCargo = (String) XssSanitizer.sanitizeValue(request.cargo());
        String safeDepartamento = (String) XssSanitizer.sanitizeValue(request.departamento());

        if (safeNombre != null) validateNombre(safeNombre);
        validateTelefono(request.telefono());

        boolean telefonoProvided = request.telefono() != null;
        boolean uebProvided = request.uebId() != null && request.uebId() > 0;
        boolean uebReset = request.uebId() != null && request.uebId() == 0;

        if (uebProvided) validateUeb(request.uebId());

        Map<String, Object> result;
        if ((telefonoProvided && request.telefono().isBlank()) || uebReset) {
            // Caso: borrar telefono y/o desasignar UEB (usar updateAllowingNulls)
            result = repository.updateAllowingNulls(
                    id,
                    safeNombre == null ? null : safeNombre.trim(),
                    safeCargo,
                    safeDepartamento,
                    emptyToNull(request.telefono()),
                    uebReset ? null : request.uebId());
        } else {
            result = repository.update(
                    id,
                    safeNombre == null ? null : safeNombre.trim(),
                    safeCargo,
                    safeDepartamento,
                    emptyToNull(request.telefono()),
                    request.uebId());
        }

        trazaService.registrarAuto("UPDATE", "EMPLEADO", id,
                "Actualización de empleado '" + existente.nombre() + "'");
        return EmpleadoMapper.toResponse(result);
    }

    @Transactional
    public EmpleadoResponse setActivo(Integer id, boolean activo) {
        findById(id);
        Map<String, Object> result = repository.setActivo(id, activo);
        trazaService.registrarAuto("UPDATE", "EMPLEADO", id,
                activo ? "Activación de empleado" : "Desactivación de empleado");
        return EmpleadoMapper.toResponse(result);
    }

    @Transactional
    public void delete(Integer id) {
        EmpleadoResponse existente = findById(id);
        if (repository.countActiveAssignments(id) > 0) {
            throw new IllegalArgumentException(
                    "No se puede eliminar: el empleado tiene asignaciones activas");
        }
        repository.delete(id);
        trazaService.registrarAuto("DELETE", "EMPLEADO", id,
                "Eliminación de empleado '" + existente.nombre() + "'");
    }

    private void validateUeb(Integer uebId) {
        if (uebId == null || uebId <= 0) {
            throw new IllegalArgumentException("La UEB es obligatoria");
        }
        if (uebRepository.findById(uebId).isEmpty()) {
            throw new ResourceNotFoundException("UEB no encontrada");
        }
    }

    private void validateNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del empleado es obligatorio");
        }
    }

    private void validateTelefono(String telefono) {
        if (telefono != null && !telefono.isBlank()
                && !telefono.matches("^[+]?[\\d\\s()\\-]{6,20}$")) {
            throw new IllegalArgumentException("El formato del teléfono no es válido");
        }
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}