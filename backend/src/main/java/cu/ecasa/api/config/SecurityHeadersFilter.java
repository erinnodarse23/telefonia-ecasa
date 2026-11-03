package cu.ecasa.api.config;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro que añade cabeceras de seguridad HTTP equivalentes a {@code helmet()}
 * de Express (Node.js).
 *
 * <p>Se ejecuta con prioridad más alta que los filtros de seguridad de Spring
 * para que TODAS las respuestas (incluidos 4xx/5xx) lleven los headers.
 *
 * @see cu.ecasa.api.config.SecurityConfig
 * @see RateLimitFilter
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private static final String CSP =
            "default-src 'self'; "
                    + "style-src 'self' 'unsafe-inline'; "
                    + "script-src 'self' 'unsafe-inline'; "
                    + "img-src 'self' data:; "
                    + "font-src 'self' data:; "
                    + "connect-src 'self'; "
                    + "frame-ancestors 'self';";

    private static final String HSTS = "max-age=31536000; includeSubDomains";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "SAMEORIGIN");
        response.setHeader("Strict-Transport-Security", HSTS);
        response.setHeader("Content-Security-Policy", CSP);
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Permissions-Policy",
                "geolocation=(), microphone=(), camera=(), payment=(), usb=()");
        response.setHeader("X-DNS-Prefetch-Control", "off");
        response.setHeader("X-Download-Options", "noopen");
        response.setHeader("X-Permitted-Cross-Domain-Policies", "none");
        response.setHeader("Cross-Origin-Embedder-Policy", "require-corp");
        response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
        response.setHeader("Cross-Origin-Resource-Policy", "same-origin");
        response.setHeader("X-XSS-Protection", "0");

        filterChain.doFilter(request, response);
    }
}