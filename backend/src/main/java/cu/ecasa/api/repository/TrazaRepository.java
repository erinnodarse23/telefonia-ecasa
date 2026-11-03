package cu.ecasa.api.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de trazas de auditoría.
 *
 * <p><b>Manejo de IP:</b> la columna {@code traza.ip} es {@code INET} en
 * PostgreSQL, pero las IPs llegan del header {@code X-Forwarded-For} o
 * {@code RemoteAddr} que pueden incluir puerto ({@code 1.2.3.4:5432}) o
 * venir múltiples separadas por coma ({@code 1.2.3.4, 10.0.0.1}).
 *
 * <p>Para evitar fallos silenciosos, la IP se <b>normaliza</b> antes de
 * intentar el cast:
 * <ul>
 *   <li>Si es {@code null} o vacía → {@code null} (NULL en BD, válido)</li>
 *   <li>Si tiene coma → se toma la primera IP (la del cliente original)</li>
 *   <li>Si tiene dos puntos y NO es IPv6 → se descarta el puerto</li>
 *   <li>Si sigue sin ser válida → {@code null} (el registro se guarda sin IP,
 *       pero no se pierde)</li>
 * </ul>
 *
 * <p>Esto garantiza que una IP malformada <b>no haga fallar toda la traza</b>.
 * La auditoría se conserva con IP {@code null} en lugar de perderse.
 *
 * @see cu.ecasa.api.service.TrazaService
 */
@Repository
public class TrazaRepository {

    private final JdbcTemplate jdbcTemplate;

    public TrazaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(Integer usuarioId, String accion, String entidad, Integer entidadId,
                     String descripcion, String ip) {
        String normalizedIp = normalizeIp(ip);
        jdbcTemplate.update(
                "INSERT INTO traza (usuario_id, accion, entidad, entidad_id, descripcion, ip) "
                        + "VALUES (?, ?, ?, ?, ?, ?::inet)",
                usuarioId, accion, entidad, entidadId,
                descripcion == null ? "" : descripcion,
                normalizedIp);
    }

    public long count(Integer usuarioId, String accion, String entidad, String desde, String hasta) {
        Filter filter = buildFilter(usuarioId, accion, entidad, desde, hasta);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM traza t" + filter.where(),
                Long.class, filter.params().toArray());
        return total == null ? 0 : total;
    }

  public List<Map<String, Object>> find(Integer usuarioId, String accion, String entidad,
                                      String desde, String hasta, int limit, int offset) {
    Filter filter = buildFilter(usuarioId, accion, entidad, desde, hasta);
    String sql = "SELECT t.id, t.usuario_id, t.accion, t.entidad, t.entidad_id, "
            + "t.descripcion, t.fecha, t.hora, "
            + "host(t.ip) AS ip, "                       // 👈 host(ip) en lugar de ip::text
            + "u.login usuario_login, u.rol usuario_rol "
            + "FROM traza t LEFT JOIN usuario u ON u.id = t.usuario_id"
            + filter.where() + " ORDER BY t.fecha DESC LIMIT ? OFFSET ?";
    List<Object> params = new ArrayList<>(filter.params());
    params.add(limit);
    params.add(offset);
    return jdbcTemplate.queryForList(sql, params.toArray());
}

    private Filter buildFilter(Integer usuarioId, String accion, String entidad, String desde, String hasta) {
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        if (usuarioId != null) { conditions.add("t.usuario_id = ?"); params.add(usuarioId); }
        if (accion != null) { conditions.add("t.accion = ?"); params.add(accion); }
        if (entidad != null) { conditions.add("t.entidad = ?"); params.add(entidad); }
        if (desde != null) { conditions.add("t.fecha >= ?::timestamp"); params.add(desde); }
        if (hasta != null) { conditions.add("t.fecha <= ?::timestamp"); params.add(hasta); }
        return new Filter(conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions), params);
    }

    /**
     * Normaliza una cadena de IP para que sea válida como {@code INET} de PostgreSQL.
     *
     * @param rawIp IP tal como llega del request (puede ser null, con puerto, con coma)
     * @return IP limpia lista para {@code ?::inet}, o {@code null} si no es recuperable
     */
    private String normalizeIp(String rawIp) {
        if (rawIp == null || rawIp.isBlank()) {
            return null;
        }

        String cleaned = rawIp.trim();

        // X-Forwarded-For puede traer "1.2.3.4, 10.0.0.1" — nos quedamos con la primera
        if (cleaned.contains(",")) {
            cleaned = cleaned.substring(0, cleaned.indexOf(',')).trim();
        }

        // IPv4 con puerto: "1.2.3.4:5432" → "1.2.3.4"
        // IPv6 no tiene puerto (usa corchetes si lo tuviera), así que solo quitamos
        // el puerto si hay UN solo ':' (IPv4:puerto). IPv6 tiene 2+.
        long colonCount = cleaned.chars().filter(ch -> ch == ':').count();
        if (colonCount == 1) {
            cleaned = cleaned.substring(0, cleaned.indexOf(':')).trim();
        }

        // Validación ligera: si queda vacía o con caracteres raros, descartamos
        if (cleaned.isEmpty() || cleaned.equalsIgnoreCase("unknown")
                || cleaned.equalsIgnoreCase("null")) {
            return null;
        }

        return cleaned;
    }

    private record Filter(String where, List<Object> params) { }
}