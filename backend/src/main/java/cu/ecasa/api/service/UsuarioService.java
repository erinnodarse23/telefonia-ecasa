package cu.ecasa.api.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cu.ecasa.api.dto.request.UsuarioRequest;
import cu.ecasa.api.dto.request.UsuarioUpdateRequest;
import cu.ecasa.api.dto.response.UsuarioResponse;
import cu.ecasa.api.mapper.UsuarioMapper;
import cu.ecasa.api.repository.UebRepository;
import cu.ecasa.api.repository.UsuarioRepository;
import cu.ecasa.api.exceptions.ResourceNotFoundException;

/**
 * Servicio de gestión de usuarios del sistema.
 *
 * <p>Trabaja con DTOs ({@link UsuarioRequest}, {@link UsuarioUpdateRequest},
 * {@link UsuarioResponse}) y delega la conversión al
 * {@link UsuarioMapper}. Todas las operaciones de escritura son transaccionales
 * y auditadas.
 *
 * @see UsuarioRepository
 * @see UsuarioMapper
 * @see TrazaService
 */
@Service
public class UsuarioService {

    private static final Set<String> ROLES = Set.of("Administrador", "GrupoComunicaciones", "JefeUnidad");
    private static final String ROL_JEFE_UNIDAD = "JefeUnidad";

    private final UsuarioRepository repository;
    private final UebRepository uebRepository;
    private final PasswordEncoder passwordEncoder;
    private final TrazaService trazaService;

    public UsuarioService(UsuarioRepository repository, UebRepository uebRepository,
                          PasswordEncoder passwordEncoder, TrazaService trazaService) {
        this.repository = repository;
        this.uebRepository = uebRepository;
        this.passwordEncoder = passwordEncoder;
        this.trazaService = trazaService;
    }

    public List<UsuarioResponse> findAll() {
        return UsuarioMapper.toResponseList(repository.findAll());
    }

    public UsuarioResponse findById(Integer id) {
        return repository.findById(id)
                .map(UsuarioMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    @Transactional
    public UsuarioResponse create(UsuarioRequest request) {
        String normalizedLogin = validateLogin(request.login());
        validatePassword(request.password());
        validateRole(request.rol());
        validateUebForRole(request.rol(), request.uebId());

        if (repository.findByLogin(normalizedLogin).isPresent()) {
            throw new IllegalArgumentException("El nombre de usuario ya existe");
        }

        Map<String, Object> result = repository.create(
                normalizedLogin,
                passwordEncoder.encode(request.password()),
                request.rol(),
                roleUeb(request.rol(), request.uebId()));

        trazaService.registrarAuto("CREATE", "USUARIO",
                ((Number) result.get("id")).intValue(),
                "Alta de usuario '" + normalizedLogin + "' (rol=" + request.rol() + ")");

        return UsuarioMapper.toResponse(result);
    }

    @Transactional
    public UsuarioResponse update(Integer id, UsuarioUpdateRequest request) {
        findById(id);

        String normalizedLogin = request.login() == null ? null : validateLogin(request.login());
        if (normalizedLogin != null) {
            repository.findByLogin(normalizedLogin).ifPresent(existing -> {
                if (!existing.get("id").equals(id)) {
                    throw new IllegalArgumentException("El nombre de usuario ya está en uso");
                }
            });
        }

        String passwordHash = null;
        if (request.password() != null && !request.password().isBlank()) {
            validatePassword(request.password());
            passwordHash = passwordEncoder.encode(request.password());
        }

        Map<String, Object> result;
        String descripcion;
        if (request.rol() != null) {
            validateRole(request.rol());
            validateUebForRole(request.rol(), request.uebId());
            Integer finalUebId = roleUeb(request.rol(), request.uebId());
            result = repository.updateWithRoleAndUeb(
                    id, normalizedLogin, passwordHash, request.rol(), request.activo(), finalUebId);
            descripcion = "Actualización de usuario (cambio de rol a " + request.rol() + ")";
        } else {
            result = repository.update(id, normalizedLogin, passwordHash, null, request.activo());
            descripcion = "Actualización parcial de usuario";
        }

        trazaService.registrarAuto("UPDATE", "USUARIO", id, descripcion);
        return UsuarioMapper.toResponse(result);
    }

    @Transactional
    public UsuarioResponse setActivo(Integer id, boolean activo) {
        findById(id);
        Map<String, Object> result = repository.update(id, null, null, null, activo);
        trazaService.registrarAuto("UPDATE", "USUARIO", id,
                activo ? "Activación de usuario" : "Desactivación de usuario");
        return UsuarioMapper.toResponse(result);
    }

    @Transactional
    public void delete(Integer id) {
        UsuarioResponse existente = findById(id);
        repository.delete(id);
        trazaService.registrarAuto("DELETE", "USUARIO", id,
                "Eliminación de usuario '" + existente.login() + "'");
    }

    // ============================================================
    // Validaciones privadas
    // ============================================================

    private String validateLogin(String login) {
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("El login no puede estar vacío");
        }
        return login.trim();
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 6 caracteres");
        }
    }

    private void validateRole(String rol) {
        if (!ROLES.contains(rol)) {
            throw new IllegalArgumentException("Rol inválido. Valores permitidos: " + ROLES);
        }
    }

    private void validateUebForRole(String rol, Integer uebId) {
        if (ROL_JEFE_UNIDAD.equals(rol) && uebId == null) {
            throw new IllegalArgumentException("ueb_id es obligatorio para el rol JefeUnidad");
        }
        if (uebId != null && uebRepository.findById(uebId).isEmpty()) {
            throw new ResourceNotFoundException("UEB no encontrada");
        }
    }

    private Integer roleUeb(String rol, Integer uebId) {
        return ROL_JEFE_UNIDAD.equals(rol) ? uebId : null;
    }
}