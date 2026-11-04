package cu.ecasa.api.service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import cu.ecasa.api.repository.TrazaRepository;
import cu.ecasa.api.security.AuthenticatedUser;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Servicio de auditoría (trazas).
 *
 * <p><b>Propagación transaccional crítica:</b> {@link #registrar} usa
 * {@code Propagation.REQUIRES_NEW} para que la auditoría se ejecute en una
 * <b>transacción independiente</b> de la operación de negocio.
 *
 * <p>Esto evita el bug clásico de PostgreSQL: si la traza falla dentro de
 * la transacción principal (aunque sea capturada), la conexión queda en
 * estado {@code aborted} y el commit final de la operación de negocio falla
 * con {@code current transaction is aborted}. Con {@code REQUIRES_NEW} la
 * traza tiene su propia conexión y, si falla, solo revierte la traza.
 *
 * @see TrazaRepository
 */
@Service
public class TrazaService {

    private static final Logger log = LoggerFactory.getLogger(TrazaService.class);

    private final TrazaRepository repository;

    public TrazaService(TrazaRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(Integer usuarioId, String accion, String entidad, Integer entidadId,
                          String descripcion, String ip) {
        try {
            repository.save(usuarioId, accion, entidad, entidadId, descripcion, ip);
        } catch (RuntimeException ex) {
            log.warn("Fallo al registrar traza (acción={}, entidad={}, entidadId={}): {}",
                    accion, entidad, entidadId, ex.getMessage());
        }
    }

    public void registrarAuto(String accion, String entidad, Integer entidadId, String descripcion) {
        registrar(extractAuthenticatedUserId(), accion, entidad, entidadId, descripcion, extractClientIp());
    }

    public void registrarAutoConUsuario(Integer usuarioId, String accion, String entidad,
                                        Integer entidadId, String descripcion) {
        registrar(usuarioId, accion, entidad, entidadId, descripcion, extractClientIp());
    }

    private Integer extractAuthenticatedUserId() {
        try {
            var authentication = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
            if (authentication != null
                    && authentication.getPrincipal() instanceof AuthenticatedUser user) {
                return user.id();
            }
        } catch (RuntimeException ex) {
            log.debug("No se pudo extraer usuario del SecurityContext: {}", ex.getMessage());
        }
        return null;
    }

    private String extractClientIp() {
        try {
            var attributes = RequestContextHolder.getRequestAttributes();
            if (attributes instanceof ServletRequestAttributes servletAttrs) {
                HttpServletRequest request = servletAttrs.getRequest();
                String forwarded = request.getHeader("X-Forwarded-For");
                if (forwarded != null && !forwarded.isBlank()) {
                    return forwarded;
                }
                return request.getRemoteAddr();
            }
        } catch (RuntimeException ex) {
            log.debug("No se pudo extraer IP del RequestContext: {}", ex.getMessage());
        }
        return null;
    }

    public Map<String, Object> listar(Integer usuarioId, String accion, String entidad,
                                      String desde, String hasta, int pagina, int limite) {
        int safePage = Math.max(1, pagina);
        int safeLimit = Math.max(1, Math.min(limite, 500));
        long total = repository.count(usuarioId, accion, entidad, desde, hasta);

        return Map.of(
                "datos", repository.find(usuarioId, accion, entidad, desde, hasta,
                        safeLimit, (safePage - 1) * safeLimit),
                "total", total,
                "pagina", safePage,
                "totalPaginas", (int) Math.ceil((double) total / safeLimit),
                "limite", safeLimit);
    }

    /**
     * Devuelve las últimas N trazas (sin paginación) para generar el PDF.
     *
     * @param usuarioId filtro por usuario (null = todos)
     * @param accion    filtro por acción (null = todas)
     * @param entidad   filtro por entidad (null = todas)
     * @param desde     fecha desde (null = sin límite)
     * @param hasta     fecha hasta (null = sin límite)
     * @param limit     máximo de filas a devolver
     * @return lista de mapas con las columnas de traza
     */
    public List<Map<String, Object>> listarParaPdf(Integer usuarioId, String accion, String entidad,
                                                   String desde, String hasta, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 10000));
        return repository.find(usuarioId, accion, entidad, desde, hasta, safeLimit, 0);
    }

    public byte[] exportar(Integer usuarioId, String accion, String entidad, String desde, String hasta) {
        List<Map<String, Object>> rows = repository.find(usuarioId, accion, entidad, desde, hasta, 10000, 0);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Trazas Auditoria");
            String[] headers = {"Fecha", "Hora", "Usuario", "Rol", "Accion", "Entidad", "Descripcion", "IP"};
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            int rowNumber = 1;
            DateTimeFormatter date = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

            for (Map<String, Object> trace : rows) {
                Row row = sheet.createRow(rowNumber++);
                Object fecha = trace.get("fecha");
                String formatted = fecha instanceof java.sql.Timestamp timestamp
                        ? timestamp.toLocalDateTime().format(date)
                        : String.valueOf(fecha == null ? "" : fecha);

                row.createCell(0).setCellValue(formatted);
                row.createCell(1).setCellValue("");
                row.createCell(2).setCellValue(String.valueOf(trace.getOrDefault("usuario_login", "")));
                row.createCell(3).setCellValue(String.valueOf(trace.getOrDefault("usuario_rol", "")));
                row.createCell(4).setCellValue(String.valueOf(trace.getOrDefault("accion", "")));
                row.createCell(5).setCellValue(String.valueOf(trace.getOrDefault("entidad", "")));
                row.createCell(6).setCellValue(String.valueOf(trace.getOrDefault("descripcion", "")));
                row.createCell(7).setCellValue(String.valueOf(trace.getOrDefault("ip", "")));
            }

            workbook.write(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo generar el Excel de trazas", exception);
        }
    }
}