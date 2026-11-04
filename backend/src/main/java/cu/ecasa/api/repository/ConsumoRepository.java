package cu.ecasa.api.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ConsumoRepository {

    private final JdbcTemplate jdbcTemplate;

    public ConsumoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> findByEquipo(Integer equipoId) {
        return jdbcTemplate.queryForList("SELECT * FROM consumo WHERE equipo_id = ? ORDER BY anio DESC, mes DESC", equipoId);
    }

    public Optional<Map<String, Object>> findById(Integer id) {
        try {
            return Optional.of(jdbcTemplate.queryForMap("SELECT * FROM consumo WHERE id = ?", id));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public Map<String, Object> update(Integer id, BigDecimal voz, BigDecimal datos, Integer sms) {
        return jdbcTemplate.queryForMap(
                "UPDATE consumo SET voz_consumido = COALESCE(?, voz_consumido), datos_consumido = COALESCE(?, datos_consumido), "
                        + "sms_consumido = COALESCE(?, sms_consumido) WHERE id = ? RETURNING *",
                voz, datos, sms, id);
    }

    /**
     * Inserta un consumo solo si no existe para el período. Usado por la
     * carga masiva desde Excel (no debe sobrescribir datos existentes).
     */
    public int insertIfAbsent(Integer equipoId, Integer anio, Integer mes, BigDecimal voz, BigDecimal datos, Integer sms) {
        return jdbcTemplate.update("INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido) "
                + "VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (equipo_id, anio, mes) DO NOTHING",
                equipoId, anio, mes, voz, datos, sms);
    }

    /**
     * Inserta o actualiza un consumo (UPSERT). Usado por el endpoint
     * {@code POST /api/consumo} para crear consumos manuales.
     *
     * <p>Si ya existe un consumo para {@code (equipo_id, anio, mes)}, lo
     * sobrescribe con los nuevos valores. Esto es intencional para el flujo
     * de creación manual, donde el operador confirma que quiere reemplazar.
     * El servicio que llama debe verificar previamente la existencia con
     * {@link #existsByEquipoAndPeriodo} para dar un 409 si no se permite
     * sobrescribir.
     *
     * @return la fila insertada o actualizada
     */
    public Map<String, Object> insertOrUpdate(Integer equipoId, Integer anio, Integer mes,
                                              BigDecimal voz, BigDecimal datos, Integer sms) {
        return jdbcTemplate.queryForMap(
                "INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido) "
                        + "VALUES (?, ?, ?, ?, ?, ?) "
                        + "ON CONFLICT (equipo_id, anio, mes) DO UPDATE SET "
                        + "voz_consumido = EXCLUDED.voz_consumido, "
                        + "datos_consumido = EXCLUDED.datos_consumido, "
                        + "sms_consumido = EXCLUDED.sms_consumido "
                        + "RETURNING *",
                equipoId, anio, mes, voz, datos, sms);
    }

    /**
     * Comprueba si ya existe un consumo para {@code (equipo_id, anio, mes)}.
     * Usado por el servicio para detectar duplicados antes de crear.
     */
    public boolean existsByEquipoAndPeriodo(Integer equipoId, Integer anio, Integer mes) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM consumo WHERE equipo_id = ? AND anio = ? AND mes = ?)",
                Boolean.class, equipoId, anio, mes);
        return Boolean.TRUE.equals(exists);
    }
}