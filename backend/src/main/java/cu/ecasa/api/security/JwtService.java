package cu.ecasa.api.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Servicio encargado de la generación y validación de tokens JWT.
 *
 * <p>Usa algoritmo HMAC-SHA256 con una clave simétrica obtenida de la
 * variable de entorno {@code JWT_SECRET}. La clave <b>debe</b> tener al menos
 * 32 caracteres (256 bits) por requisito del algoritmo; si no, la aplicación
 * no arranca.
 *
 * <p>Estructura del token:
 * <ul>
 *   <li>{@code jti}: identificador único del token (UUID v4), usado para revocación</li>
 *   <li>{@code sub}: login del usuario</li>
 *   <li>{@code id}: ID numérico del usuario</li>
 *   <li>{@code rol}: rol del usuario (Administrador, GrupoComunicaciones, JefeUnidad)</li>
 *   <li>{@code ueb_id}: UEB asignada (puede ser null)</li>
 *   <li>{@code iat}, {@code exp}: issued-at y expiration</li>
 * </ul>
 *
 * @see JwtAuthenticationFilter
 * @see AuthenticatedUser
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    /** Longitud mínima del secreto para HS256 (256 bits / 8 = 32 bytes). */
    private static final int MIN_SECRET_LENGTH = 32;

    /** Duración por defecto del token (1 hora). */
    private static final Duration DEFAULT_DURATION = Duration.ofHours(1);

    /** Duración del token con "recordarme" (7 días). */
    private static final Duration REMEMBER_ME_DURATION = Duration.ofDays(7);

    private final SecretKey signingKey;

    /**
     * Construye el servicio validando la configuración del secreto.
     *
     * @param secret valor de la propiedad {@code app.jwt-secret}
     * @throws IllegalStateException si el secreto no está configurado o es demasiado corto
     */
    public JwtService(@Value("${app.jwt-secret}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET no configurado. Defina la variable de entorno con al menos "
                            + MIN_SECRET_LENGTH + " caracteres.");
        }
        if (secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "JWT_SECRET debe tener al menos " + MIN_SECRET_LENGTH
                            + " caracteres (actual: " + secret.length() + ").");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        log.info("JwtService inicializado. Algoritmo: HS256, duración por defecto: {}h, rememberMe: {}d",
                DEFAULT_DURATION.toHours(), REMEMBER_ME_DURATION.toDays());
    }

    /**
     * Genera un nuevo JWT firmado para el usuario indicado.
     *
     * @param user       usuario autenticado
     * @param rememberMe si {@code true}, la expiración se extiende a 7 días
     * @return el token JWT compacto listo para enviar en {@code Authorization: Bearer <token>}
     */
    public String generateToken(AuthenticatedUser user, boolean rememberMe) {
        String jti = user.jti() != null ? user.jti() : UUID.randomUUID().toString();
        Duration duration = rememberMe ? REMEMBER_ME_DURATION : DEFAULT_DURATION;
        Date now = new Date();
        Date expiration = new Date(now.getTime() + duration.toMillis());

        return Jwts.builder()
                .id(jti)
                .subject(user.login())
                .claim("id", user.id())
                .claim("login", user.login())
                .claim("rol", user.rol())
                .claim("ueb_id", user.uebId())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Verifica la firma y expiración del token y extrae sus claims en un
     * {@link AuthenticatedUser}.
     *
     * @param token JWT compacto (sin prefijo {@code Bearer })
     * @return el usuario autenticado con {@code jti} incluido
     * @throws JwtException si el token es inválido, expiró o tiene firma incorrecta
     */
    public AuthenticatedUser parse(String token) {
        Jws<Claims> parsed = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token);
        Claims claims = parsed.getPayload();

        Number id = claims.get("id", Number.class);
        Number uebId = claims.get("ueb_id", Number.class);

        return new AuthenticatedUser(
                id == null ? null : id.intValue(),
                claims.get("login", String.class),
                claims.get("rol", String.class),
                uebId == null ? null : uebId.intValue(),
                claims.getId());
    }

    /**
     * Extrae los claims completos del token sin construir el {@link AuthenticatedUser}.
     * Útil para operaciones como logout (que solo necesitan {@code jti} y {@code exp}).
     *
     * @param token JWT compacto
     * @return los claims del payload
     * @throws JwtException si el token es inválido o expiró
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Comprueba si un token es válido (firma correcta y no expirado) sin lanzar excepción.
     *
     * @param token JWT compacto
     * @return {@code true} si el token puede ser verificado, {@code false} en caso contrario
     */
    public boolean isValid(String token) {
        try {
            Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }
}