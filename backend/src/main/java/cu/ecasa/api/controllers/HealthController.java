package cu.ecasa.api.controllers;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health check de la API.
 *
 * <p>Verifica:
 * <ul>
 *   <li>Estado del servidor (siempre {@code ok} si responde)</li>
 *   <li>Conexión a la base de datos ({@code db: up/down})</li>
 *   <li>Uptime en segundos desde el arranque</li>
 *   <li>Timestamp ISO-8601 del momento de la respuesta</li>
 * </ul>
 *
 * <p>Si la BD no responde, devuelve {@code 503 Service Unavailable}.
 * Este endpoint es público (no requiere autenticación) y es el que usan
 * Render, UptimeRobot, cron-job.org, etc. para keep-alive.
 *
 * <p>Mantiene el campo {@code message} por compatibilidad con el contrato
 * anterior del endpoint (tests existentes, frontend que lo consuma, etc.).
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final long startTime = System.currentTimeMillis();

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        long uptimeSeconds = (System.currentTimeMillis() - startTime) / 1000;
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            body.put("status", "ok");
            body.put("message", "Servidor funcionando");
            body.put("db", "up");
            body.put("uptime", uptimeSeconds);
            body.put("timestamp", Instant.now().toString());
            return ResponseEntity.ok(body);
        } catch (Exception exception) {
            body.put("status", "error");
            body.put("message", "Servidor funcionando");
            body.put("db", "down");
            body.put("uptime", uptimeSeconds);
            body.put("timestamp", Instant.now().toString());
            return ResponseEntity.status(503).body(body);
        }
    }
}