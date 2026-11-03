package cu.ecasa.api.security;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import cu.ecasa.api.repository.RevokedTokenRepository;
import cu.ecasa.api.repository.UsuarioRepository;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro que intercepta cada request HTTP para autenticar al usuario a partir
 * del header {@code Authorization: Bearer <token>}.
 *
 * <p>Flujo:
 * <ol>
 *   <li>Si no hay header o no empieza con {@code Bearer }, deja pasar la request
 *       sin autenticar (endpoints públicos como {@code /api/health} funcionan;
 *       endpoints protegidos serán rechazados por {@code SecurityConfig}).</li>
 *   <li>Parsea el token y extrae el {@link AuthenticatedUser}.</li>
 *   <li>Rechaza tokens revocados (logout explícito) consultando
 *       {@link RevokedTokenRepository}.</li>
 *   <li>Rechaza tokens de usuarios desactivados o eliminados consultando
 *       {@link UsuarioRepository}.</li>
 *   <li>Si todo es válido, registra la autenticación en el {@code SecurityContext}.</li>
 * </ol>
 *
 * <p>Los errores se manejan internamente: la request continúa sin autenticar
 * (el {@code ExceptionTranslationFilter} de Spring Security decide el código
 * HTTP final, normalmente 401). Se loguean a {@code debug} para diagnóstico
 * sin saturar los logs en producción.
 *
 * @see JwtService
 * @see cu.ecasa.api.config.SecurityConfig
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtService jwtService;
    private final RevokedTokenRepository revokedTokenRepository;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   RevokedTokenRepository revokedTokenRepository,
                                   UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.revokedTokenRepository = revokedTokenRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(AUTH_HEADER);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();

        try {
            AuthenticatedUser user = jwtService.parse(token);

            // (1) El token debe estar vigente en la lista de revocados
            if (revokedTokenRepository.isRevoked(user.jti())) {
                log.debug("Token revocado rechazado. jti={}, login={}", user.jti(), user.login());
                filterChain.doFilter(request, response);
                return;
            }

            // (2) El usuario debe seguir activo y existir
            Optional<Map<String, Object>> usuarioOpt = usuarioRepository.findById(user.id());
            if (usuarioOpt.isEmpty() || !Boolean.TRUE.equals(usuarioOpt.get().get("activo"))) {
                log.debug("Usuario inactivo o inexistente. id={}, login={}", user.id(), user.login());
                filterChain.doFilter(request, response);
                return;
            }

            // (3) Autenticación válida: registrar en el contexto
            var authentication = new UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    List.of(new SimpleGrantedAuthority(ROLE_PREFIX + user.rol())));
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (JwtException ex) {
            // Token malformado, expirado o con firma incorrecta
            log.debug("JWT inválido: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
        } catch (RuntimeException ex) {
            // Error inesperado (BD caída, datos corruptos) — se ve en logs
            log.warn("Error inesperado procesando JWT", ex);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}