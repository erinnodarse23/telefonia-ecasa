package cu.ecasa.api.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro que escribe un access log HTTP en formato Apache Combined,
 * equivalente a Morgan (formato dev).
 *
 * <p><b>Decisiones de diseÃ±o:</b>
 * <ul>
 *   <li>Usa {@link ZonedDateTime} en lugar de {@link java.time.LocalDateTime}
 *       porque el patrÃ³n {@code Z} (offset) requiere una zona horaria.
 *       {@code LocalDateTime} no tiene offset â†’ lanzarÃ­a
 *       {@code UnsupportedTemporalTypeException}.</li>
 *   <li>{@link Locale#ENGLISH} explÃ­cito para que el nombre del mes siempre
 *       sea en inglÃ©s ({@code Nov}), no traducido ({@code nov}) â€” coherente
 *       con el formato Apache estÃ¡ndar.</li>
 *   <li>{@code catch (Exception)} (no solo {@code IOException}) para que
 *       CUALQUIER fallo al escribir el log (NPE, IllegalState, etc.) se loguee
 *       a WARN y NUNCA se propague al cliente. Un fallo de logging no debe
 *       romper el request HTTP.</li>
 * </ul>
 *
 * @see SecurityHeadersFilter
 * @see RateLimitFilter
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class AccessLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AccessLogFilter.class);

    /** Formato Apache Combined: 30/Nov/2026:07:15:40 -0500 (offset obligatorio â†’ ZonedDateTime). */
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH);

    private static final DateTimeFormatter FILE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final Path logDir;

    public AccessLogFilter() {
        this.logDir = Paths.get("logs").toAbsolutePath().normalize();
        try {
            Files.createDirectories(logDir);
            log.info("AccessLogFilter activo. Directorio de logs: {}", logDir);
        } catch (Exception e) {
            log.error("AccessLogFilter: no se pudo crear el directorio de logs {}", logDir, e);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        long start = System.currentTimeMillis();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            writeAccessLogSafely(request, response, duration);
        }
    }

    /**
     * Escribe el access log. Cualquier excepciÃ³n se captura y loguea a WARN,
     * pero NUNCA se propaga al cliente.
     */
    private void writeAccessLogSafely(HttpServletRequest request,
                                       HttpServletResponse response,
                                       long durationMs) {
        try {
            String ip = extractClientIp(request);
            // âœ… ZonedDateTime en lugar de LocalDateTime (tiene offset para el patrÃ³n 'Z')
            String timestamp = ZonedDateTime.now().format(DATE_FORMAT);
            String method = request.getMethod();
            String uri = request.getRequestURI();
            String query = request.getQueryString();
            String path = query != null ? uri + "?" + query : uri;
            String protocol = request.getProtocol();
            int status = response.getStatus();

            long size = 0L;
            try {
                String contentLength = response.getHeader("Content-Length");
                if (contentLength != null && !contentLength.isBlank()) {
                    size = Long.parseLong(contentLength);
                }
            } catch (NumberFormatException ignored) {
                // header invÃ¡lido, se deja size en 0
            }

            String line = String.format(
                    "%s - - [%s] \"%s %s %s\" %d %d %d%n",
                    ip, timestamp, method, path, protocol, status, size, durationMs);

            Path dailyFile = logDir.resolve("access-" + LocalDate.now().format(FILE_DATE_FORMAT) + ".log");
            Files.writeString(dailyFile, line,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        } catch (Exception e) {
            // âš ï¸ catch (Exception) no (IOException): cualquier RuntimeException
            // (NPE, UnsupportedTemporalTypeException, IllegalState) se captura.
            // NUNCA propagar al cliente. WARN para visibilidad en el log del backend.
            log.warn("AccessLogFilter: no se pudo escribir el access log: {}", e.getMessage());
        }
    }

    private String extractClientIp(HttpServletRequest request) {
    try {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        String remote = request.getRemoteAddr();
        // CosmÃ©tico: comprimir IPv6 localhost "0:0:0:0:0:0:0:1" â†’ "::1"
        if ("0:0:0:0:0:0:0:1".equals(remote)) return "::1";
        if ("0:0:0:0:0:0:0:0".equals(remote)) return "::";
        return remote;
    } catch (Exception e) {
        return "-";
    }
}
}