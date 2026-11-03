package cu.ecasa.api.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PlanRepository {

    private final JdbcTemplate jdbcTemplate;

    public PlanRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> findAll() {
        return jdbcTemplate.queryForList("SELECT * FROM plan ORDER BY fecha_inicio DESC");
    }

    public List<Map<String, Object>> findByEquipo(Integer equipoId) {
        return jdbcTemplate.queryForList("SELECT * FROM plan WHERE equipo_id = ? ORDER BY fecha_inicio DESC", equipoId);
    }

    public Optional<Map<String, Object>> findById(Integer id) {
        try {
            return Optional.of(jdbcTemplate.queryForMap("SELECT * FROM plan WHERE id = ?", id));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public boolean hasActiveByEquipo(Integer equipoId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM plan WHERE equipo_id = ? AND fecha_fin IS NULL", Integer.class, equipoId);
        return count != null && count > 0;
    }

    public Map<String, Object> create(Integer equipoId, Integer voz, Integer datos, Integer sms, BigDecimal cuota, LocalDate fechaInicio) {
        return jdbcTemplate.queryForMap(
                "INSERT INTO plan (equipo_id, voz_plan, datos_plan, sms_plan, cuota, fecha_inicio) VALUES (?, ?, ?, ?, ?, ?) RETURNING *",
                equipoId, voz, datos, sms, cuota, fechaInicio);
    }

    public Map<String, Object> update(Integer id, Integer voz, Integer datos, Integer sms, BigDecimal cuota) {
        return jdbcTemplate.queryForMap(
                "UPDATE plan SET voz_plan = COALESCE(?, voz_plan), datos_plan = COALESCE(?, datos_plan), "
                        + "sms_plan = COALESCE(?, sms_plan), cuota = COALESCE(?, cuota) WHERE id = ? RETURNING *",
                voz, datos, sms, cuota, id);
    }

    public Map<String, Object> finalizePlan(Integer id, LocalDate fechaFin) {
        return jdbcTemplate.queryForMap("UPDATE plan SET fecha_fin = ? WHERE id = ? RETURNING *", fechaFin, id);
    }

    public void delete(Integer id) {
        jdbcTemplate.update("DELETE FROM plan WHERE id = ?", id);
    }
}