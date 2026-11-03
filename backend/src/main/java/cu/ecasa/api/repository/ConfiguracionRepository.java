package cu.ecasa.api.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de configuración del sistema (umbrales, planes anuales, parámetros).
 *
 * <p>La tabla {@code configuracion} admite tres tipos de entrada según los
 * valores de {@code anio} y {@code ueb_id}:
 * <ol>
 *   <li><b>Global:</b> {@code anio IS NULL AND ueb_id IS NULL} — configuración
 *       común a todo el sistema (ej: {@code UMBRAL}, límites globales)</li>
 *   <li><b>Por año:</b> {@code anio NOT NULL AND ueb_id IS NULL} — datos anuales
 *       comunes a todas las UEBs (ej: {@code DATOS_ANTERIORES})</li>
 *   <li><b>Por año y UEB:</b> {@code anio NOT NULL AND ueb_id NOT NULL} —
 *       configuración específica por UEB (ej: {@code PLAN_ANUAL})</li>
 * </ol>
 *
 * <p><b>Importante sobre NULL y UPSERT:</b> PostgreSQL trata {@code NULL != NULL}
 * en constraints UNIQUE. Por eso la tabla usa <b>3 índices únicos parciales</b>
 * en lugar de un solo constraint:
 * <ul>
 *   <li>{@code uk_config_global} — {@code (tipo)} WHERE {@code anio IS NULL AND ueb_id IS NULL}</li>
 *   <li>{@code uk_config_anual} — {@code (tipo, anio)} WHERE {@code anio IS NOT NULL AND ueb_id IS NULL}</li>
 *   <li>{@code uk_config_anual_ueb} — {@code (tipo, anio, ueb_id)} WHERE {@code anio IS NOT NULL AND ueb_id IS NOT NULL}</li>
 * </ul>
 * Los {@code INSERT ... ON CONFLICT} usan la cláusula {@code WHERE} del índice
 * parcial correspondiente para que el UPSERT funcione con NULLs.
 *
 * @see cu.ecasa.api.service.ConfiguracionService
 * @see scripts/08_configuracion_upsert.sql
 */
@Repository
public class ConfiguracionRepository {

    private final JdbcTemplate jdbcTemplate;

    public ConfiguracionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ============================================================
    // Consultas
    // ============================================================

    public List<Map<String, Object>> annualPlans(Integer year) {
        return jdbcTemplate.queryForList(
                "SELECT id, ueb_id, anio, monto FROM plan_anual WHERE anio = ? ORDER BY ueb_id", year);
    }

    public Map<String, Object> annualPlan(Integer uebId, Integer year) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, ueb_id, anio, monto FROM plan_anual WHERE ueb_id = ? AND anio = ?", uebId, year);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Map<String, Object> jsonConfig(String type, Integer year) {
        List<Map<String, Object>> rows = year == null
                ? jdbcTemplate.queryForList(
                        "SELECT * FROM configuracion WHERE tipo = ? AND anio IS NULL AND ueb_id IS NULL LIMIT 1", type)
                : jdbcTemplate.queryForList(
                        "SELECT * FROM configuracion WHERE tipo = ? AND anio = ? AND ueb_id IS NULL LIMIT 1", type, year);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<Map<String, Object>> thresholdsByUeb() {
        return jdbcTemplate.queryForList(
                "SELECT id ueb_id, umbral_sobregasto porcentaje FROM ueb WHERE activo = true ORDER BY nombre");
    }

    // ============================================================
    // Mutaciones
    // ============================================================

    public Map<String, Object> saveAnnualPlan(Integer uebId, Integer year, BigDecimal amount) {
        return jdbcTemplate.queryForMap(
                "INSERT INTO plan_anual (ueb_id, anio, monto) VALUES (?, ?, ?) "
                        + "ON CONFLICT (ueb_id, anio) DO UPDATE SET monto = EXCLUDED.monto "
                        + "RETURNING id, ueb_id, anio, monto",
                uebId, year, amount);
    }

    /**
     * Guarda o actualiza un valor JSON de configuración.
     *
     * <p>Delega en {@link #saveJsonConfig(String, Integer, Integer, String)}
     * pasando {@code uebId = null} (configuración global o por año, sin UEB).
     *
     * @param type tipo de configuración (ej: {@code "UMBRAL"})
     * @param year año ({@code null} para configuración global)
     * @param json valor JSON serializado
     */
    public void saveJsonConfig(String type, Integer year, String json) {
        saveJsonConfig(type, year, null, json);
    }

    /**
     * Guarda o actualiza un valor JSON de configuración con UPSERT atómico.
     *
     * <p>El comportamiento depende de {@code year} y {@code uebId}:
     * <ul>
     *   <li>Ambos {@code null} → configuración global (índice {@code uk_config_global})</li>
     *   <li>{@code year} presente, {@code uebId} {@code null} → por año
     *       (índice {@code uk_config_anual})</li>
     *   <li>Ambos presentes → por año y UEB (índice {@code uk_config_anual_ueb})</li>
     * </ul>
     *
     * <p>Es <b>atómico</b>: dos requests concurrentes con la misma clave
     * nunca producirán filas duplicadas. Si dos llegan a la vez, PostgreSQL
     * serializa el UPSERT y una de las transacciones hace UPDATE.
     *
     * @param type  tipo de configuración
     * @param year  año ({@code null} para configuración global)
     * @param uebId ID de UEB ({@code null} para configuración no específica por UEB)
     * @param json  valor JSON serializado
     * @throws IllegalArgumentException si {@code uebId} está definido pero {@code year} no
     */
    public void saveJsonConfig(String type, Integer year, Integer uebId, String json) {
        if (uebId != null && year == null) {
            throw new IllegalArgumentException(
                    "Si se especifica uebId, year también debe estar presente");
        }

        if (year == null) {
            // Caso 1: global (anio NULL, ueb_id NULL)
            jdbcTemplate.update(
                    "INSERT INTO configuracion (tipo, anio, ueb_id, valor_json) "
                            + "VALUES (?, NULL, NULL, ?::jsonb) "
                            + "ON CONFLICT (tipo) WHERE anio IS NULL AND ueb_id IS NULL "
                            + "DO UPDATE SET valor_json = EXCLUDED.valor_json, "
                            + "  updated_at = CURRENT_TIMESTAMP",
                    type, json);
        } else if (uebId == null) {
            // Caso 2: por año, sin UEB
            jdbcTemplate.update(
                    "INSERT INTO configuracion (tipo, anio, ueb_id, valor_json) "
                            + "VALUES (?, ?, NULL, ?::jsonb) "
                            + "ON CONFLICT (tipo, anio) WHERE anio IS NOT NULL AND ueb_id IS NULL "
                            + "DO UPDATE SET valor_json = EXCLUDED.valor_json, "
                            + "  updated_at = CURRENT_TIMESTAMP",
                    type, year, json);
        } else {
            // Caso 3: por año y UEB
            jdbcTemplate.update(
                    "INSERT INTO configuracion (tipo, anio, ueb_id, valor_json) "
                            + "VALUES (?, ?, ?, ?::jsonb) "
                            + "ON CONFLICT (tipo, anio, ueb_id) WHERE anio IS NOT NULL AND ueb_id IS NOT NULL "
                            + "DO UPDATE SET valor_json = EXCLUDED.valor_json, "
                            + "  updated_at = CURRENT_TIMESTAMP",
                    type, year, uebId, json);
        }
    }

    public void saveThreshold(Integer uebId, BigDecimal value) {
        jdbcTemplate.update("UPDATE ueb SET umbral_sobregasto = ? WHERE id = ?", value, uebId);
    }
}