package cu.ecasa.api.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cu.ecasa.api.config.XssSanitizer;
import cu.ecasa.api.dto.request.AsignacionRequest;
import cu.ecasa.api.dto.request.FinalizarAsignacionRequest;
import cu.ecasa.api.dto.response.AsignacionResponse;
import cu.ecasa.api.mapper.AsignacionMapper;
import cu.ecasa.api.repository.AsignacionRepository;
import cu.ecasa.api.repository.EmpleadoRepository;
import cu.ecasa.api.repository.EquipoRepository;
import cu.ecasa.api.exceptions.ResourceNotFoundException;

/**
 * Servicio de gestión de asignaciones equipo→empleado con DTOs.
 *
 * <p><b>Reglas de negocio validadas:</b>
 * <ul>
 *   <li>Equipo y empleado deben existir y estar activos</li>
 *   <li>Un equipo solo puede tener una asignación activa</li>
 *   <li>Un empleado solo puede tener una asignación activa</li>
 *   <li>La fecha de inicio no puede ser futura</li>
 * </ul>
 *
 * <p><b>Efectos colaterales:</b> al crear la asignación se actualiza el
 * teléfono del empleado con el número de línea. Al finalizar o eliminar
 * se limpia.
 *
 * @see AsignacionRepository
 * @see AsignacionMapper
 * @see XssSanitizer
 */
@Service
public class AsignacionService {

    private final AsignacionRepository repository;
    private final EquipoRepository equipoRepository;
    private final EmpleadoRepository empleadoRepository;

    public AsignacionService(AsignacionRepository repository,
                             EquipoRepository equipoRepository,
                             EmpleadoRepository empleadoRepository) {
        this.repository = repository;
        this.equipoRepository = equipoRepository;
        this.empleadoRepository = empleadoRepository;
    }

    public List<AsignacionResponse> findAll(String estado) {
        return AsignacionMapper.toResponseList(repository.findAll(estado == null ? "activas" : estado));
    }

    public List<AsignacionResponse> findByEquipo(Integer equipoId, String estado) {
        requireEquipo(equipoId);
        return AsignacionMapper.toResponseList(
                repository.findByEquipo(equipoId, estado == null ? "activas" : estado));
    }

    public List<AsignacionResponse> findByEmpleado(Integer empleadoId, String estado) {
        requireEmpleado(empleadoId);
        return AsignacionMapper.toResponseList(
                repository.findByEmpleado(empleadoId, estado == null ? "activas" : estado));
    }

    public AsignacionResponse findById(Integer id) {
        return repository.findById(id)
                .map(AsignacionMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Asignación no encontrada"));
    }

    @Transactional
    public AsignacionResponse create(AsignacionRequest request) {
        Map<String, Object> equipo = requireEquipo(request.equipoId());
        Map<String, Object> empleado = requireEmpleado(request.empleadoId());

        if (!Boolean.TRUE.equals(equipo.get("activo"))) {
            throw new IllegalArgumentException("No se puede asignar un equipo inactivo");
        }
        if (!Boolean.TRUE.equals(empleado.get("activo"))) {
            throw new IllegalArgumentException("No se puede asignar a un empleado inactivo");
        }
        if (repository.hasActiveByEquipo(request.equipoId())) {
            throw new IllegalArgumentException("El equipo ya está asignado");
        }
        if (repository.hasActiveByEmpleado(request.empleadoId())) {
            throw new IllegalArgumentException("El empleado ya tiene una línea activa");
        }
        if (request.fechaInicio() == null) {
            throw new IllegalArgumentException("La fecha de inicio es obligatoria");
        }
        if (request.fechaInicio().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser futura");
        }

        String safeEstadoEntrega = (String) XssSanitizer.sanitizeValue(request.estadoEquipoEntrega());

        Map<String, Object> result = repository.create(
                request.equipoId(), request.empleadoId(),
                request.fechaInicio(), safeEstadoEntrega);
        empleadoRepository.updateTelefono(
                request.empleadoId(), String.valueOf(equipo.get("numero_linea")));
        return AsignacionMapper.toResponse(result);
    }

    @Transactional
    public AsignacionResponse finalizar(Integer id, FinalizarAsignacionRequest request) {
        AsignacionResponse assignment = findById(id);
        if (assignment.fechaFin() != null) {
            throw new IllegalArgumentException("La asignación ya está finalizada");
        }
        if (request.fechaFin() == null) {
            throw new IllegalArgumentException("La fecha de fin es obligatoria");
        }

        String safeEstadoDevolucion = (String) XssSanitizer.sanitizeValue(request.estadoEquipoDevolucion());

        Map<String, Object> result = repository.finalizar(
                id, request.fechaFin(), safeEstadoDevolucion);
        empleadoRepository.updateTelefono(assignment.empleadoId(), null);
        return AsignacionMapper.toResponse(result);
    }

    @Transactional
    public void delete(Integer id) {
        AsignacionResponse assignment = findById(id);
        repository.delete(id);
        empleadoRepository.updateTelefono(assignment.empleadoId(), null);
    }

    private Map<String, Object> requireEquipo(Integer id) {
        return equipoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo no encontrado"));
    }

    private Map<String, Object> requireEmpleado(Integer id) {
        return empleadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado"));
    }
}