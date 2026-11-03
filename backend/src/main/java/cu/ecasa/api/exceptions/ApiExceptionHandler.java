package cu.ecasa.api.exceptions;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import cu.ecasa.api.service.AuthService.AuthException;

/**
 * Manejador global de excepciones para todos los controladores REST.
 *
 * <p>Traduce las excepciones lanzadas por la aplicación a respuestas HTTP
 * con un formato JSON uniforme:
 * <pre>{@code {"error": "mensaje legible", "status": 400}}</pre>
 * El frontend consume el mismo formato sin importar si el error vino de
 * una validación, de autenticación o de la base de datos.
 *
 * <p><b>Mapa de excepciones a códigos HTTP:</b>
 * <ul>
 *   <li>{@link AuthException} → 401, 429 o 400 según {@code exception.status()}</li>
 *   <li>{@link AccessDeniedException} → 403 (Spring Security)</li>
 *   <li>{@link ResourceNotFoundException}, {@link NoResourceFoundException} → 404</li>
 *   <li>{@link IllegalArgumentException}, {@link MethodArgumentTypeMismatchException},
 *       {@link MissingServletRequestParameterException},
 *       {@link HttpMessageNotReadableException},
 *       {@link MethodArgumentNotValidException} → 400</li>
 *   <li>{@link DataIntegrityViolationException} → 409</li>
 *   <li>Cualquier otra {@link Exception} → 500 (con log a nivel ERROR)</li>
 * </ul>
 *
 * <p><b>Seguridad:</b> nunca se devuelve stacktrace ni mensaje técnico al cliente
 * en errores 500. El detalle completo se loguea en el servidor.
 *
 * @see ApiErrorWriter
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private static final String MSG_INTERNAL = "Error interno del servidor";
    private static final String MSG_FORBIDDEN = "Acceso denegado";
    private static final String MSG_NOT_FOUND = "Ruta no encontrada";
    private static final String MSG_MALFORMED_JSON = "JSON malformado o inválido";
    private static final String MSG_CONFLICT = "El recurso ya existe o viola una restricción de la base de datos";

    // ============================================================
    // 401 / 403 / 429 — Autenticación y autorización
    // ============================================================

    /**
     * Excepciones de autenticación con código HTTP embebido (login, captcha, bloqueo).
     */
    @ExceptionHandler(AuthException.class)
    ResponseEntity<Map<String, Object>> handleAuth(AuthException ex) {
        HttpStatus status = HttpStatus.resolve(ex.status());
        if (status == null) {
            status = HttpStatus.UNAUTHORIZED;
        }
        log.debug("AuthException ({}): {}", status.value(), ex.getMessage());
        return build(status, ex.getMessage());
    }

    /**
     * Excepción de Spring Security cuando el usuario autenticado no tiene el rol requerido.
     */
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        log.debug("Acceso denegado: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, MSG_FORBIDDEN);
    }

    // ============================================================
    // 404 — Recursos no encontrados
    // ============================================================

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<Map<String, Object>> handleNoResource(NoResourceFoundException ex) {
        log.debug("Ruta no encontrada: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, MSG_NOT_FOUND);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.debug("Recurso no encontrado: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // ============================================================
    // 400 — Datos de entrada inválidos
    // ============================================================

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        log.warn("Argumento inválido: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex) {
        log.warn("JSON malformado: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, MSG_MALFORMED_JSON);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String expected = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "desconocido";
        log.warn("Tipo inválido en parámetro '{}': {}", ex.getName(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST,
                "Parámetro inválido '" + ex.getName() + "': se esperaba " + expected);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException ex) {
        log.warn("Parámetro requerido faltante: {}", ex.getParameterName());
        return build(HttpStatus.BAD_REQUEST,
                "Parámetro requerido faltante: " + ex.getParameterName());
    }

    /**
     * Errores de validación de Bean Validation ({@code @Valid} en DTOs).
     * Devuelve 400 con el detalle por campo.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err -> fieldErrors.put(
                err.getField(),
                err.getDefaultMessage() != null ? err.getDefaultMessage() : "inválido"));

        log.warn("Validación fallida: {} campo(s)", fieldErrors.size());

        Map<String, Object> body = baseBody(HttpStatus.BAD_REQUEST, "Validación fallida");
        body.put("fields", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    // ============================================================
    // 409 — Conflicto (constraint de BD)
    // ============================================================

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        // El mensaje específico de BD es útil en logs pero NUNCA al cliente.
        String cause = ex.getMostSpecificCause().getMessage();
        log.warn("Violación de integridad de BD: {}", cause);
        return build(HttpStatus.CONFLICT, MSG_CONFLICT);
    }

    // ============================================================
    // 500 — Catch-all (fallo inesperado)
    // ============================================================

    /**
     * Captura cualquier excepción no manejada.
     *
     * <p><b>Importante:</b> se loguea a nivel ERROR con stacktrace completo
     * para diagnóstico en producción. El cliente solo recibe un mensaje
     * genérico sin detalles técnicos.
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        log.error("Excepción no manejada", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, MSG_INTERNAL);
    }

    // ============================================================
    // Helpers
    // ============================================================

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(baseBody(status, message));
    }

    private Map<String, Object> baseBody(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", message);
        body.put("status", status.value());
        return body;
    }
}