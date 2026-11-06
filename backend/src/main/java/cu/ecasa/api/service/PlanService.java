package cu.ecasa.api.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cu.ecasa.api.dto.request.FinalizePlanRequest;
import cu.ecasa.api.dto.request.PlanRequest;
import cu.ecasa.api.dto.request.PlanUpdateRequest;
import cu.ecasa.api.dto.response.PlanResponse;
import cu.ecasa.api.mapper.PlanMapper;
import cu.ecasa.api.repository.EquipoRepository;
import cu.ecasa.api.repository.PlanRepository;
import cu.ecasa.api.exceptions.ResourceNotFoundException;

/**
 * Servicio de gestiÃ³n de planes tarifarios con DTOs y transacciones.
 *
 * <p><b>Comportamiento al crear:</b> si el equipo ya tiene un plan activo,
 * se cierra automÃ¡ticamente con fecha de ayer antes de crear el nuevo
 * (decisiÃ³n de negocio alineada con el backend Java).
 *
 * @see PlanRepository
 * @see PlanMapper
 */
@Service
public class PlanService {

    private final PlanRepository repository;
    private final EquipoRepository equipoRepository;

    public PlanService(PlanRepository repository, EquipoRepository equipoRepository) {
        this.repository = repository;
        this.equipoRepository = equipoRepository;
    }

    public List<PlanResponse> findAll() {
        return PlanMapper.toResponseList(repository.findAll());
    }

    public List<PlanResponse> findByEquipo(Integer equipoId) {
        requireEquipo(equipoId);
        return PlanMapper.toResponseList(repository.findByEquipo(equipoId));
    }

    public PlanResponse findById(Integer id) {
        return repository.findById(id)
                .map(PlanMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Plan no encontrado"));
    }

    @Transactional
    public PlanResponse create(PlanRequest request) {
        requireEquipo(request.equipoId());
        validateCuota(request.cuota());
        if (request.fechaInicio() == null) {
            throw new IllegalArgumentException("La fecha de inicio es obligatoria");
        }

        // Auto-cierre del plan anterior si existe (decisiÃ³n de negocio).
        // Se cierra con fecha de ayer para no solapar con el nuevo plan.
        if (repository.hasActiveByEquipo(request.equipoId())) {
            LocalDate ayer = LocalDate.now().minusDays(1);
            repository.finalizarPlanActivo(request.equipoId(), ayer);
        }

        Map<String, Object> result = repository.create(
                request.equipoId(),
                request.vozPlan(),
                request.datosPlan(),
                request.smsPlan(),
                request.cuota(),
                request.fechaInicio());
        return PlanMapper.toResponse(result);
    }

    @Transactional
    public PlanResponse update(Integer id, PlanUpdateRequest request) {
        findById(id);
        validateCuota(request.cuota());

        Map<String, Object> result = repository.update(
                id, request.vozPlan(), request.datosPlan(), request.smsPlan(), request.cuota());
        return PlanMapper.toResponse(result);
    }

    @Transactional
    public void delete(Integer id) {
        findById(id);
        repository.delete(id);
    }

    @Transactional
    public PlanResponse finalizePlan(Integer id, FinalizePlanRequest request) {
        PlanResponse plan = findById(id);
        if (plan.fechaFin() != null) {
            throw new IllegalArgumentException("El plan ya estÃ¡ finalizado");
        }
        if (request.fechaFin() == null) {
            throw new IllegalArgumentException("La fecha de fin es obligatoria");
        }

        Map<String, Object> result = repository.finalizePlan(id, request.fechaFin());
        return PlanMapper.toResponse(result);
    }

    private void requireEquipo(Integer id) {
        if (equipoRepository.findById(id).isEmpty()) {
            throw new ResourceNotFoundException("Equipo no encontrado");
        }
    }

    private void validateCuota(BigDecimal cuota) {
        if (cuota != null && cuota.signum() <= 0) {
            throw new IllegalArgumentException("La cuota debe ser mayor que 0");
        }
    }
}