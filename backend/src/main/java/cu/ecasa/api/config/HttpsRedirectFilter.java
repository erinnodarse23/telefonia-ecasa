package cu.ecasa.api.config;

import java.io.IOException;

import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro que fuerza HTTPS en producción, redirigiendo con 301 cualquier
 * request HTTP que no llegue con {@code X-Forwarded-Proto: https}.
 *
 * <p>Solo se activa con perfil {@code prod}. En {@code dev} permite HTTP.
 */
@Component
@Profile("prod")
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class HttpsRedirectFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String proto = request.getHeader("X-Forwarded-Proto");
        boolean isHttps = "https".equalsIgnoreCase(proto) || request.isSecure();

        if (!isHttps) {
            String host = request.getHeader("Host");
            String uri = request.getRequestURI();
            String query = request.getQueryString();
            String url = "https://" + host + uri + (query != null ? "?" + query : "");
            response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            response.setHeader("Location", url);
            return;
        }

        filterChain.doFilter(request, response);
    }
}