package cu.ecasa.api.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Utilidad para parsear valores de celdas Excel de forma robusta.
 *
 * <p>Excel puede producir valores en múltiples formatos según la configuración
 * regional del sistema operativo del usuario. Esta clase detecta y maneja:
 * <ul>
 *   <li><b>Decimales:</b> formato US ({@code 1,234.56}), formato EU
 *       ({@code 1.234,56}), sin separador de miles ({@code 1234.56} o
 *       {@code 1234,56}), y enteros ({@code 1234})</li>
 *   <li><b>Fechas:</b> ISO ({@code 2025-01-15}), EU ({@code 15/01/2025}),
 *       US ({@code 01/15/2025}), y variantes con guion</li>
 * </ul>
 *
 * <p><b>Estrategia:</b> se prueban varios formatos en orden. Si todos fallan,
 * se lanza {@link IllegalArgumentException} con mensaje legible para el usuario.
 *
 * @see cu.ecasa.api.service.CargaService
 * @see cu.ecasa.api.service.ImportacionService
 */
public final class ExcelValueParser {

    /** Formatos de fecha soportados, ordenados por prioridad. */
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ISO_LOCAL_DATE,                                  // 2025-01-15
            DateTimeFormatter.ofPattern("d/M/yyyy"),                            // 15/1/2025 o 15/01/2025
            DateTimeFormatter.ofPattern("M/d/yyyy"),                            // 1/15/2025 o 01/15/2025
            DateTimeFormatter.ofPattern("d-M-yyyy"),                            // 15-1-2025
            DateTimeFormatter.ofPattern("M-d-yyyy"),                            // 1-15-2025
            DateTimeFormatter.ofPattern("d.M.yyyy")                             // 15.1.2025
    };

    private ExcelValueParser() {
        // Utility class
    }

    /**
     * Parsea un valor decimal desde una celda Excel.
     *
     * <p>Maneja los formatos:
     * <ul>
     *   <li>{@code "1234.56"} → 1234.56 (US sin miles)</li>
     *   <li>{@code "1234,56"} → 1234.56 (EU sin miles)</li>
     *   <li>{@code "1,234.56"} → 1234.56 (US con miles)</li>
     *   <li>{@code "1.234,56"} → 1234.56 (EU con miles)</li>
     *   <li>{@code "1,234"} → 1234 (miles US, 3 dígitos después de coma sin más comas)</li>
     *   <li>{@code ""} o {@code null} → {@link BigDecimal#ZERO}</li>
     * </ul>
     *
     * @param value cadena tal como la produce Excel
     * @return el valor decimal
     * @throws IllegalArgumentException si no se puede parsear como número
     */
    public static BigDecimal parseDecimal(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }

        String cleaned = value.trim();

        // Intento directo: si ya es "1234.56", funciona sin tocar nada
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException ignored) {
            // Continúa con la detección de formato
        }

        boolean hasComma = cleaned.contains(",");
        boolean hasDot = cleaned.contains(".");

        if (hasComma && hasDot) {
            // Ambos separadores: el ÚLTIMO es el decimal
            int lastComma = cleaned.lastIndexOf(',');
            int lastDot = cleaned.lastIndexOf('.');

            if (lastComma > lastDot) {
                // Formato EU: 1.234,56 → quitar puntos de miles, coma a punto
                cleaned = cleaned.replace(".", "").replace(",", ".");
            } else {
                // Formato US: 1,234.56 → quitar comas de miles
                cleaned = cleaned.replace(",", "");
            }
        } else if (hasComma) {
            // Solo coma: distinguir entre miles US (1,234) y decimal EU (1234,56)
            String[] parts = cleaned.split(",");
            if (parts.length == 2 && parts[1].length() == 3) {
                // "1,234" → 1234 (miles US)
                cleaned = cleaned.replace(",", "");
            } else {
                // "1234,56" → 1234.56 (decimal EU)
                cleaned = cleaned.replace(",", ".");
            }
        }
        // Si solo hay punto, ya está en formato correcto para BigDecimal

        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Valor no numérico: '" + value + "'", ex);
        }
    }

    /**
     * Parsea un valor entero desde una celda Excel.
     *
     * @param value cadena tal como la produce Excel
     * @return el valor entero, o {@code 0} si la cadena está vacía
     * @throws IllegalArgumentException si el valor no es numérico o tiene decimales
     */
    public static Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        BigDecimal decimal = parseDecimal(value);
        try {
            return decimal.intValueExact();
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException(
                    "El valor no es un entero: '" + value + "' (parseado como " + decimal + ")", ex);
        }
    }

    /**
     * Parsea una fecha desde una celda Excel.
     *
     * <p>Prueba múltiples formatos hasta encontrar uno válido:
     * ISO ({@code 2025-01-15}), EU ({@code 15/01/2025}),
     * US ({@code 01/15/2025}), con guion y con punto.
     *
     * @param value cadena tal como la produce Excel
     * @return la fecha parseada, o {@code null} si la cadena está vacía
     * @throws IllegalArgumentException si ningún formato coincide
     */
    public static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String cleaned = value.trim();

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(cleaned, formatter);
            } catch (DateTimeParseException ignored) {
                // Prueba el siguiente formato
            }
        }

        throw new IllegalArgumentException("Fecha no válida: '" + value
                + "' (formatos soportados: ISO yyyy-MM-dd, EU dd/MM/yyyy, US MM/dd/yyyy)");
    }
}