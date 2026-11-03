package cu.ecasa.api.repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de alertas del sistema.
 *
 * <p>Gestiona el CRUD de alertas y las consultas auxiliares necesarias
 * para el módulo de generación automática ({@link cu.ecasa.api.service.AlertGenerationService}).
 *
 * <p>Una alerta representa una advertencia persistente asociada a una UEB
 * y un período (año, mes). La tabla {@code alerta} tiene una constraint
 * {@code UNIQUE (ueb_id, anio, mes, tipo)} que garantiza idempotencia:
 * no puede haber dos alertas idénticas para el mismo período.
 *
 * @see cu.ecasa.api.service.AlertaService
 * @see cu.ecasa.api.service.AlertGenerationService
 */
@Repository
public class AlertaRepository {

    private final JdbcTemplate jdbcTemplate;

    public AlertaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ============================================================
    // Consultas de lectura
    // ============================================================

    public long count(String tipo, Boolean leida, Boolean soloActivas, Integer uebId) {
        Filter filter = filter(tipo, leida, soloActivas, uebId);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM alerta a" + filter.where(),
                Long.class, filter.params().toArray());
        return total == null ? 0 : total;
    }

    public List<Map<String, Object>> find(String tipo, Boolean leida, Boolean soloActivas,
                                          Integer uebId, int limit, int offset) {
        Filter filter = filter(tipo, leida, soloActivas, uebId);
        String sql = "SELECT a.*, u.codigo ueb_codigo, u.nombre ueb_nombre "
                + "FROM alerta a LEFT JOIN ueb u ON u.id = a.ueb_id"
                + filter.where() + " ORDER BY a.creada_en DESC LIMIT ? OFFSET ?";
        List<Object> params = new ArrayList<>(filter.params());
        params.add(limit);
        params.add(offset);
        return jdbcTemplate.queryForList(sql, params.toArray());
    }

    public List<Map<String, Object>> recent(int limit, Integer uebId) {
        String sql = "SELECT a.*, u.codigo ueb_codigo, u.nombre ueb_nombre "
                + "FROM alerta a LEFT JOIN ueb u ON u.id = a.ueb_id "
                + "WHERE a.descartada = false"
                + (uebId == null ? "" : " AND a.ueb_id = ?")
                + " ORDER BY a.creada_en DESC LIMIT ?";
        return uebId == null
                ? jdbcTemplate.queryForList(sql, limit)
                : jdbcTemplate.queryForList(sql, uebId, limit);
    }

    private Filter filter(String tipo, Boolean leida, Boolean soloActivas, Integer uebId) {
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        if (tipo != null) { conditions.add("a.tipo = ?"); params.add(tipo); }
        if (leida != null) { conditions.add("a.leida = ?"); params.add(leida); }
        if (Boolean.TRUE.equals(soloActivas)) { conditions.add("a.descartada = false"); }
        if (uebId != null) { conditions.add("a.ueb_id = ?"); params.add(uebId); }
        return new Filter(conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions), params);
    }

    // ============================================================
    // Mutaciones
    // ============================================================

    public Map<String, Object> markRead(Integer id) {
        return update("UPDATE alerta SET leida = true WHERE id = ? RETURNING *", id);
    }

    public Map<String, Object> discard(Integer id) {
        return update("UPDATE alerta SET descartada = true WHERE id = ? RETURNING *", id);
    }

    private Map<String, Object> update(String sql, Integer id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public int markAllRead(Integer uebId) {
        String sql = "UPDATE alerta SET leida = true "
                + "WHERE leida = false AND descartada = false"
                + (uebId == null ? "" : " AND ueb_id = ?");
        return uebId == null ? jdbcTemplate.update(sql) : jdbcTemplate.update(sql, uebId);
    }

    public int archiveAll(Integer uebId) {
        String sql = "UPDATE alerta SET descartada = true "
                + "WHERE descartada = false"
                + (uebId == null ? "" : " AND ueb_id = ?");
        return uebId == null ? jdbcTemplate.update(sql) : jdbcTemplate.update(sql, uebId);
    }

    // ============================================================
    // Métodos para el módulo de generación automática de alertas
    // ============================================================

    /**
     * Obtiene las UEBs activas que tienen un umbral de sobregasto configurado.
     * Las UEBs sin umbral (o con umbral &le; 0) se excluyen porque no deben
     * generar alertas automáticas.
     *
     * @return lista de mapas con {@code id}, {@code codigo}, {@code nombre},
     *         {@code umbral_sobregasto}
     */
    public List<Map<String, Object>> findUebsConUmbral() {
        return jdbcTemplate.queryForList(
                "SELECT id, codigo, nombre, umbral_sobregasto "
                        + "FROM ueb "
                        + "WHERE activo = true "
                        + "  AND umbral_sobregasto IS NOT NULL "
                        + "  AND umbral_sobregasto > 0 "
                        + "ORDER BY codigo");
    }

    /**
     * Comprueba si ya existe una alerta para una UEB, año, mes y tipo concretos.
     * Garantiza idempotencia: el job de evaluación no debe duplicar alertas
     * en ejecuciones sucesivas durante el mismo período.
     *
     * @param uebId ID de la UEB
     * @param anio  año del período
     * @param mes   mes del período (1-12)
     * @param tipo  tipo de alerta (ej: {@code "SOBREGASTO"})
     * @return {@code true} si ya existe una alerta con esa clave única
     */
    public boolean existsByPeriodo(Integer uebId, Integer anio, Integer mes, String tipo) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM alerta "
                        + "WHERE ueb_id = ? AND anio = ? AND mes = ? AND tipo = ?)",
                Boolean.class,
                uebId, anio, mes, tipo);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Inserta una nueva alerta en la base de datos.
     *
     * <p>La constraint {@code UNIQUE (ueb_id, anio, mes, tipo)} de la tabla
     * {@code alerta} protege contra duplicados en caso de concurrencia. Si se
     * intenta insertar una alerta ya existente, se lanzará
     * {@code DataIntegrityViolationException}; el servicio que llama debe
     * validar previamente con {@link #existsByPeriodo}.
     *
     * @param uebId      ID de la UEB afectada
     * @param anio       año del período
     * @param mes        mes del período (1-12)
     * @param tipo       tipo de alerta
     * @param porcentaje porcentaje de consumo respecto al plan anual
     * @param mensaje    mensaje descriptivo para el usuario
     */
    public void insert(Integer uebId, Integer anio, Integer mes, String tipo,
                       BigDecimal porcentaje, String mensaje) {
        jdbcTemplate.update(
                "INSERT INTO alerta (ueb_id, anio, mes, tipo, porcentaje, mensaje, leida, descartada) "
                        + "VALUES (?, ?, ?, ?, ?, ?, false, false)",
                uebId, anio, mes, tipo, porcentaje, mensaje);
    }

    /**
     * Calcula el consumo acumulado de una UEB en un año concreto.
     *
     * <p>Suma {@code voz_consumido + datos_consumido} de todos los consumos
     * del año, asociados a equipos cuya asignación activa pertenece a un
     * empleado de la UEB indicada. Esto refleja el costo acumulado de la UEB
     * en el año y es la base para el cálculo de porcentaje de sobregasto.
     *
     * @param uebId ID de la UEB
     * @param anio  año a evaluar
     * @return suma acumulada; {@code BigDecimal.ZERO} si no hay datos
     */
    public BigDecimal calcularConsumoAcumuladoPorUeb(Integer uebId, Integer anio) {
        BigDecimal total = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(c.voz_consumido + c.datos_consumido), 0) "
                        + "FROM consumo c "
                        + "JOIN equipo e ON e.id = c.equipo_id "
                        + "JOIN asignacion a ON a.equipo_id = e.id AND a.fecha_fin IS NULL "
                        + "JOIN empleado emp ON emp.id = a.empleado_id "
                        + "WHERE emp.ueb_id = ? AND c.anio = ?",
                BigDecimal.class,
                uebId, anio);
        return total == null ? BigDecimal.ZERO : total;
    }

    public record Filter(String where, List<Object> params) { }
}