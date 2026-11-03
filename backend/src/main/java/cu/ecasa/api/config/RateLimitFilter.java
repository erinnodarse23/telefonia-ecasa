package cu.ecasa.api.config;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro de rate limiting por IP con 3 niveles, equivalente a los limiters
 * de {@code express-rate-limit} en el backend Node.
 *
 * <p><b>Niveles:</b>
 * <ol>
 *   <li><b>Login</b> ({@code /api/auth/login}, {@code /api/usuarios/login}):
 *       10 req/min por IP</li>
 *   <li><b>Carga</b> ({@code /api/carga}): 10 req/min por IP</li>
 *   <li><b>Global</b> ({@code /api/**}): 2000 req/15min por IP</li>
 * </ol>
 *
 * <p><b>Nota:</b> rate limiter en memoria. Para producción con múltiples
 * instancias, migrar a Redis o Bucket4j con backend distribuido.
 *
 * @see SecurityHeadersFilter
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final long LOGIN_WINDOW_MS = 60_000L;
    private static final int LOGIN_MAX_REQ = 10;

    private static final long CARGA_WINDOW_MS = 60_000L;
    private static final int CARGA_MAX_REQ = 10;

    private static final long GLOBAL_WINDOW_MS = 15 * 60_000L;
    private static final int GLOBAL_MAX_REQ = 2000;

    private final ConcurrentHashMap<String, Window> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String ip = extractClientIp(request);

        Limit limit = chooseLimit(path);

        if (limit != null && !allow(limit.kind + "|" + ip, limit.windowMs, limit.maxReq)) {
            log.warn("Rate limit excedido: tipo={}, ip={}, path={}", limit.kind, ip, path);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Retry-After", String.valueOf(limit.windowMs / 1000));
            response.getWriter().write(
                    "{\"error\":\"" + limit.message + "\",\"status\":429}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private Limit chooseLimit(String path) {
        if (path.equals("/api/auth/login") || path.equals("/api/usuarios/login")) {
            return new Limit("login", LOGIN_WINDOW_MS, LOGIN_MAX_REQ,
                    "Demasiados intentos de inicio de sesión, intente más tarde");
        }
        if (path.startsWith("/api/carga")) {
            return new Limit("carga", CARGA_WINDOW_MS, CARGA_MAX_REQ,
                    "Demasiadas cargas de archivos, intente más tarde");
        }
        if (path.startsWith("/api/")) {
            return new Limit("global", GLOBAL_WINDOW_MS, GLOBAL_MAX_REQ,
                    "Demasiadas solicitudes, intente más tarde");
        }
        return null;
    }

    private boolean allow(String key, long windowMs, int maxReq) {
        long now = System.currentTimeMillis();
        Window w = buckets.computeIfAbsent(key, k -> new Window(now, new AtomicInteger(0)));

        synchronized (w) {
            if (now - w.windowStart >= windowMs) {
                w.windowStart = now;
                w.count.set(0);
            }
            return w.count.incrementAndGet() <= maxReq;
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }

    private record Limit(String kind, long windowMs, int maxReq, String message) { }

    private static final class Window {
        private volatile long windowStart;
        private final AtomicInteger count;

        private Window(long windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}