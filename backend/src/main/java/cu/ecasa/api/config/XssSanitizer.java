package cu.ecasa.api.config;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

/**
 * Utilidad de sanitizaciÃ³n XSS equivalente a {@code utils/sanitize.js} del
 * backend Java.
 *
 * <p>Usa OWASP Java HTML Sanitizer con una polÃ­tica estricta: elimina
 * <b>todas</b> las etiquetas HTML y conserva solo el texto plano. Replica
 * exactamente el comportamiento de
 * de sanitización XSS por defecto.
 *
 * <p><b>Campos sanitizables por defecto</b> (por defecto):
 * {@code descripcion}, {@code observaciones}, {@code nombre}, {@code cargo},
 * {@code departamento}, {@code marca}, {@code modelo},
 * {@code estado_equipo_entrega}, {@code estado_equipo_devolucion}.
 *
 * @see cu.ecasa.api.service.UebService
 * @see cu.ecasa.api.service.EmpleadoService
 * @see cu.ecasa.api.service.EquipoService
 * @see cu.ecasa.api.service.AsignacionService
 */
public final class XssSanitizer {

    /**
     * PolÃ­tica estricta: sin etiquetas HTML permitidas, solo texto plano.
     *
     * <p>Equivalente a {@code ALLOWED_TAGS: []} de DOMPurify. Todo tag HTML
     * es eliminado, pero el contenido textual se conserva.
     */
    private static final PolicyFactory STRICT_POLICY = new HtmlPolicyBuilder().toFactory();

    private XssSanitizer() {
        // Utility class
    }

    /**
     * Sanitiza un valor escalar. Si no es un String, lo devuelve tal cual.
     * Si es un String, elimina todas las etiquetas HTML y devuelve el texto limpio.
     *
     * @param value valor a sanitizar
     * @return texto sin etiquetas HTML, o {@code value} sin cambios si no es String
     */
    public static Object sanitizeValue(Object value) {
        if (!(value instanceof String s)) {
            return value;
        }
        return STRICT_POLICY.sanitize(s.trim());
    }
}