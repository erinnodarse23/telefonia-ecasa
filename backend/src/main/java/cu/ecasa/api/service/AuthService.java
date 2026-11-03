package cu.ecasa.api.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import cu.ecasa.api.dto.request.LoginRequest;
import cu.ecasa.api.dto.response.CaptchaResponse;
import cu.ecasa.api.dto.response.LoginResponse;
import cu.ecasa.api.dto.response.UsuarioResponse;
import cu.ecasa.api.dto.response.VerifyResponse;
import cu.ecasa.api.mapper.UsuarioMapper;
import cu.ecasa.api.repository.RevokedTokenRepository;
import cu.ecasa.api.repository.UsuarioRepository;
import cu.ecasa.api.security.AuthenticatedUser;
import cu.ecasa.api.security.JwtService;

/**
 * Servicio de autenticación con DTOs.
 *
 * @see JwtService
 * @see RevokedTokenRepository
 * @see TrazaService
 * @see UsuarioMapper
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final int CAPTCHA_THRESHOLD = 3;
    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 15 * 60 * 1000L;
    private static final long BLOCK_MS = 15 * 60 * 1000L;
    private static final long CAPTCHA_TTL_MS = 5 * 60 * 1000L;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RevokedTokenRepository revokedTokenRepository;
    private final TrazaService trazaService;

    private final Map<String, AttemptState> attempts = new ConcurrentHashMap<>();
    private final Map<String, Captcha> captchas = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RevokedTokenRepository revokedTokenRepository,
                       TrazaService trazaService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.revokedTokenRepository = revokedTokenRepository;
        this.trazaService = trazaService;
    }

    public LoginResponse login(LoginRequest request) {
        String login = request.login();
        if (login == null || login.isBlank() || request.password() == null || request.password().isBlank()) {
            throw new AuthException("Login y contraseña son obligatorios", 400);
        }

        AttemptState state = attempts.computeIfAbsent(login, ignored -> new AttemptState());
        state.clean();

        if (state.blockedUntil > System.currentTimeMillis()) {
            long segundos = (state.blockedUntil - System.currentTimeMillis()) / 1000;
            log.warn("Login bloqueado para '{}'. Faltan {}s", login, segundos);
            throw new AuthException("Demasiados intentos fallidos. Intente de nuevo en "
                    + (segundos / 60) + " min", 429);
        }

        if (state.failures.size() >= CAPTCHA_THRESHOLD) {
            if (!verifyCaptcha(request.captchaId(), request.captchaAnswer())) {
                throw new AuthException(request.captchaId() == null
                        ? "Credenciales inválidas" : "Captcha incorrecto", 401);
            }
        }

        var row = usuarioRepository.findByLogin(login).orElse(null);
        boolean passwordMatches = row != null
                && passwordEncoder.matches(request.password(), String.valueOf(row.get("password_hash")));

        if (row == null || !Boolean.TRUE.equals(row.get("activo")) || !passwordMatches) {
            state.registerFailure();
            log.warn("Login fallido para '{}'. Intentos: {}", login, state.failures.size());

            Integer userId = row != null ? ((Number) row.get("id")).intValue() : null;
            String motivo = row == null ? "usuario no encontrado"
                    : !Boolean.TRUE.equals(row.get("activo")) ? "usuario desactivado"
                    : "contraseña incorrecta";
            trazaService.registrarAutoConUsuario(userId, "LOGIN_FAILED", "AUTH", userId,
                    "Login fallido para '" + login + "' (" + motivo + ")");

            throw new AuthException("Credenciales inválidas", 401);
        }

        attempts.remove(login);
        AuthenticatedUser user = toUser(row);
        String token = jwtService.generateToken(user, Boolean.TRUE.equals(request.rememberMe()));

        log.info("Login exitoso. login={}, rol={}, ueb={}, rememberMe={}",
                login, user.rol(), user.uebId(), request.rememberMe());
        trazaService.registrarAutoConUsuario(user.id(), "LOGIN", "AUTH", user.id(),
                "Login exitoso (rol=" + user.rol() + ")");

        return new LoginResponse(token, UsuarioMapper.toResponse(row));
    }

    public VerifyResponse verify(Integer userId) {
        var row = usuarioRepository.findById(userId)
                .orElseThrow(() -> new AuthException("Usuario no encontrado", 401));
        return new VerifyResponse(true, UsuarioMapper.toResponse(row));
    }

    public void logout(String token) {
        try {
            var claims = jwtService.parseClaims(token);
            Instant expiration = claims.getExpiration().toInstant();
            Integer userId = claims.get("id", Number.class).intValue();

            revokedTokenRepository.revoke(claims.getId(), userId, Date.from(expiration));
            log.info("Logout. usuarioId={}, jti={}", userId, claims.getId());

            trazaService.registrarAutoConUsuario(userId, "LOGOUT", "AUTH", userId,
                    "Cierre de sesión");
        } catch (Exception ex) {
            log.debug("Logout con token inválido o expirado: {}", ex.getMessage());
        }
    }

    public CaptchaResponse generateCaptcha() {
        int a = 1 + secureRandom.nextInt(9);
        int b = 1 + secureRandom.nextInt(9);
        String id = UUID.randomUUID().toString();

        captchas.put(id, new Captcha(String.valueOf(a + b),
                System.currentTimeMillis() + CAPTCHA_TTL_MS));

        return new CaptchaResponse(id, "¿Cuánto es " + a + " + " + b + "?");
    }

    private boolean verifyCaptcha(String id, String answer) {
        if (id == null || answer == null) {
            return false;
        }
        Captcha captcha = captchas.remove(id);
        return captcha != null
                && captcha.expiresAt > System.currentTimeMillis()
                && captcha.answer.equals(answer.trim());
    }

    private AuthenticatedUser toUser(Map<String, Object> row) {
        Number id = (Number) row.get("id");
        Number uebId = (Number) row.get("ueb_id");
        return new AuthenticatedUser(
                id.intValue(),
                String.valueOf(row.get("login")),
                String.valueOf(row.get("rol")),
                uebId == null ? null : uebId.intValue(),
                null);
    }

    public static class AuthException extends RuntimeException {
        private final int status;

        public AuthException(String message, int status) {
            super(message);
            this.status = status;
        }

        public int status() {
            return status;
        }
    }

    private static final class AttemptState {
        private final CopyOnWriteArrayList<Long> failures = new CopyOnWriteArrayList<>();
        private volatile long blockedUntil;

        private void clean() {
            long cutoff = System.currentTimeMillis() - WINDOW_MS;
            failures.removeIf(timestamp -> timestamp <= cutoff);
            if (blockedUntil <= System.currentTimeMillis()) {
                blockedUntil = 0;
            }
        }

        private void registerFailure() {
            failures.add(System.currentTimeMillis());
            clean();
            if (failures.size() >= MAX_ATTEMPTS) {
                blockedUntil = System.currentTimeMillis() + BLOCK_MS;
            }
        }
    }

    private record Captcha(String answer, long expiresAt) {
    }
}