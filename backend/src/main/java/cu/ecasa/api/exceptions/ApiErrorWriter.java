package cu.ecasa.api.exceptions;


import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Utilidad para escribir respuestas de error consistentes en formato JSON
 * desde cualquier punto de la cadena de filtros o controllers.
 *
 * <p>Se usa desde dos lugares:
 * <ul>
 *   <li>{@code SecurityConfig} (pre-filtros de seguridad: 401, 403)</li>
 *   <li>{@code ApiExceptionHandler} (handlers de excepciones de aplicación)</li>
 * </ul>
 *
 * <p>Garantiza un formato uniforme {@code {"error": "...", "status": N}}
 * que el frontend consume sin cambios entre backend Node y Java.
 */
public final class ApiErrorWriter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ApiErrorWriter() {
        // Utility class
    }

    /**
     * Escribe una respuesta de error JSON en el {@link HttpServletResponse}.
     *
     * @param response respuesta HTTP a la que se escribe
     * @param status   código HTTP (401, 403, 400, 404, 500...)
     * @param message  mensaje legible para el usuario
     * @throws IOException si falla la escritura al cliente
     */
    public static void write(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", message);
        body.put("status", status);

        MAPPER.writeValue(response.getOutputStream(), body);
    }
}