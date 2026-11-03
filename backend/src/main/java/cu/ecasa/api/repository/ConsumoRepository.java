package cu.ecasa.api.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;

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

    public int insertIfAbsent(Integer equipoId, Integer anio, Integer mes, BigDecimal voz, BigDecimal datos, Integer sms) {
        return jdbcTemplate.update("INSERT INTO consumo (equipo_id, anio, mes, voz_consumido, datos_consumido, sms_consumido) "
                + "VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (equipo_id, anio, mes) DO NOTHING",
                equipoId, anio, mes, voz, datos, sms);
    }
}