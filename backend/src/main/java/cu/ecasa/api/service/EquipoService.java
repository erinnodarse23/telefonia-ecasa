package cu.ecasa.api.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cu.ecasa.api.dto.request.EquipoRequest;
import cu.ecasa.api.dto.request.EquipoUpdateRequest;
import cu.ecasa.api.dto.response.EquipoResponse;
import cu.ecasa.api.mapper.EquipoMapper;
import cu.ecasa.api.repository.EquipoRepository;
import cu.ecasa.api.exceptions.ResourceNotFoundException;
import cu.ecasa.api.config.XssSanitizer;

/**
 * Servicio de gestión de equipos con DTOs, auditoría y sanitización XSS.
 *
 * @see EquipoRepository
 * @see EquipoMapper
 * @see TrazaService
 * @see XssSanitizer
 */
@Service
public class EquipoService {

    private static final Set<String> ESTADOS = Set.of("nuevo", "bueno", "dañado", "de baja");
    private static final String ESTADO_DEFAULT = "bueno";

    private final EquipoRepository repository;
    private final TrazaService trazaService;

    public EquipoService(EquipoRepository repository, TrazaService trazaService) {
        this.repository = repository;
        this.trazaService = trazaService;
    }

    public List<EquipoResponse> findAll(Boolean activo, String estado) {
        if (estado != null) {
            validateEstado(estado);
            return EquipoMapper.toResponseList(repository.findByEstado(estado));
        }
        return EquipoMapper.toResponseList(repository.findAll(activo));
    }

    public EquipoResponse findById(Integer id) {
        return repository.findById(id)
                .map(EquipoMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo no encontrado"));
    }

    public EquipoResponse findByNumero(String numero) {
        return repository.findByNumero(numero)
                .map(EquipoMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo no encontrado"));
    }

    @Transactional
    public EquipoResponse create(EquipoRequest request) {
        String normalized = validateNumero(request.numeroLinea());

        String safeMarca = (String) XssSanitizer.sanitizeValue(request.marca());
        String safeModelo = (String) XssSanitizer.sanitizeValue(request.modelo());
        String safeObservaciones = (String) XssSanitizer.sanitizeValue(request.observaciones());

        if (repository.findByNumero(normalized).isPresent()) {
            throw new IllegalArgumentException("El número de línea ya existe");
        }

        String finalEstado = request.estadoFisico() == null || request.estadoFisico().isBlank()
                ? ESTADO_DEFAULT
                : request.estadoFisico();
        validateEstado(finalEstado);

        Map<String, Object> result = repository.create(
                normalized, safeMarca, safeModelo, finalEstado, safeObservaciones);

        trazaService.registrarAuto("CREATE", "EQUIPO",
                ((Number) result.get("id")).intValue(),
                "Alta de equipo línea '" + normalized + "' (" + safeMarca + " " + safeModelo + ")");
        return EquipoMapper.toResponse(result);
    }

    @Transactional
    public EquipoResponse update(Integer id, EquipoUpdateRequest request) {
        EquipoResponse existente = findById(id);

        String normalized = request.numeroLinea() == null ? null : validateNumero(request.numeroLinea());
        String safeMarca = (String) XssSanitizer.sanitizeValue(request.marca());
        String safeModelo = (String) XssSanitizer.sanitizeValue(request.modelo());
        String safeObservaciones = (String) XssSanitizer.sanitizeValue(request.observaciones());

        if (normalized != null) {
            repository.findByNumero(normalized).ifPresent(existing -> {
                if (!existing.get("id").equals(id)) {
                    throw new IllegalArgumentException("El número de línea ya está asignado a otro equipo");
                }
            });
        }
        if (request.estadoFisico() != null) {
            validateEstado(request.estadoFisico());
        }

        Map<String, Object> result = repository.update(
                id, normalized, safeMarca, safeModelo, request.estadoFisico(), safeObservaciones);

        trazaService.registrarAuto("UPDATE", "EQUIPO", id,
                "Actualización de equipo línea '" + existente.numeroLinea() + "'");
        return EquipoMapper.toResponse(result);
    }

    @Transactional
    public EquipoResponse setActivo(Integer id, boolean activo) {
        findById(id);
        Map<String, Object> result = repository.setActivo(id, activo);
        trazaService.registrarAuto("UPDATE", "EQUIPO", id,
                activo ? "Activación de equipo" : "Desactivación de equipo");
        return EquipoMapper.toResponse(result);
    }

    @Transactional
    public void delete(Integer id) {
        EquipoResponse existente = findById(id);
        if (repository.countActiveAssignments(id) > 0) {
            throw new IllegalArgumentException(
                    "No se puede eliminar el equipo porque tiene asignaciones activas. Finalícelas primero.");
        }
        if (repository.countActivePlans(id) > 0) {
            throw new IllegalArgumentException(
                    "No se puede eliminar el equipo porque tiene planes activos. Finalícelos primero.");
        }
        repository.delete(id);
        trazaService.registrarAuto("DELETE", "EQUIPO", id,
                "Eliminación de equipo línea '" + existente.numeroLinea() + "'");
    }

    private String validateNumero(String numero) {
        if (numero == null || numero.isBlank()) {
            throw new IllegalArgumentException("El número de línea es obligatorio");
        }
        return numero.trim();
    }

    private void validateEstado(String estado) {
        if (!ESTADOS.contains(estado)) {
            throw new IllegalArgumentException("Estado inválido. Valores permitidos: " + ESTADOS);
        }
    }
}