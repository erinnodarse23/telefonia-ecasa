package cu.ecasa.api.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cu.ecasa.api.security.AuthenticatedUser;
import cu.ecasa.api.service.DashboardService;
import cu.ecasa.api.service.ExportacionService;
import cu.ecasa.api.service.PdfService;
import cu.ecasa.api.service.ReporteService;
import cu.ecasa.api.service.TrazaService;

@RestController
@RequestMapping("/api/exportar")
@PreAuthorize("hasAnyRole('Administrador', 'GrupoComunicaciones')")
public class ExportacionController {

    private static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private final ExportacionService exportacion;
    private final ReporteService reportes;
    private final DashboardService dashboard;
    private final PdfService pdf;
    private final TrazaService trazas;

    public ExportacionController(ExportacionService exportacion, ReporteService reportes,
                                 DashboardService dashboard, PdfService pdf, TrazaService trazas) {
        this.exportacion = exportacion;
        this.reportes = reportes;
        this.dashboard = dashboard;
        this.pdf = pdf;
        this.trazas = trazas;
    }

    @GetMapping("/responsables/excel")
    public ResponseEntity<byte[]> responsables(@RequestParam(required = false) Integer uebId) {
        List<Map<String, Object>> rows = reportes.responsables(uebId);
        byte[] data = exportacion.excel("Responsables", new String[]{"Empleado", "Cargo", "Teléfono", "Línea", "Marca", "Modelo", "Fecha Asignación"}, rows);
        return download(data, "responsables_UEB.xlsx");
    }

    @GetMapping("/equipos/excel")
    public ResponseEntity<byte[]> equipos(@RequestParam(required = false) String estado) {
        List<Map<String, Object>> rows = reportes.equiposPorEstado(estado, null);
        byte[] data = exportacion.excel("Equipos", new String[]{"Número de Línea", "Marca", "Modelo", "Estado Físico", "Observaciones"}, rows);
        return download(data, "equipos_estado_" + (estado == null ? "todos" : estado) + ".xlsx");
    }

    @GetMapping("/responsables/pdf")
    public ResponseEntity<byte[]> responsablesPdf(@RequestParam(required = false) Integer uebId) {
        byte[] data = pdf.responsables(reportes.responsables(uebId), "Responsables por UEB");
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header("Content-Disposition", "attachment; filename=responsables_UEB.pdf").body(data);
    }

    @GetMapping("/equipos/pdf")
    public ResponseEntity<byte[]> equiposPdf(@RequestParam(required = false) String estado) {
        byte[] data = pdf.equipos(reportes.equiposPorEstado(estado, null), "Equipos por Estado");
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header("Content-Disposition", "attachment; filename=equipos_estado.pdf").body(data);
    }

    @GetMapping("/dashboard/excel")
    public ResponseEntity<byte[]> dashboard(@RequestParam int anio, Authentication authentication) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        Integer uebId = "JefeUnidad".equals(user.rol()) ? user.uebId() : null;
        byte[] data = exportacion.excel("Dashboard",
                new String[]{"UEB", "Servicios", "% Datos", "Plan Anual", "Prom. Mensual", "Est. Cierre", "Diferencia", "% Sobregasto"},
                dashboard.summary(anio, uebId));
        return download(data, "dashboard_" + anio + ".xlsx");
    }

    /**
     * PDF del dashboard: tabla de indicadores por UEB.
     */
    @GetMapping("/dashboard/pdf")
    public ResponseEntity<byte[]> dashboardPdf(@RequestParam int anio, Authentication authentication) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        Integer uebId = "JefeUnidad".equals(user.rol()) ? user.uebId() : null;
        byte[] data = pdf.dashboard(dashboard.summary(anio, uebId), "Dashboard", anio);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=dashboard_" + anio + ".pdf").body(data);
    }

    /**
     * PDF de trazas de auditoría. Devuelve las últimas {@code limit} trazas.
     */
    @GetMapping("/trazas/pdf")
    public ResponseEntity<byte[]> trazasPdf(@RequestParam(defaultValue = "500") Integer limit) {
        List<Map<String, Object>> rows = trazas.listarParaPdf(null, null, null, null, null, limit);
        byte[] data = pdf.trazas(rows, "Trazas de Auditoría");
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=trazas_auditoria.pdf").body(data);
    }

    private ResponseEntity<byte[]> download(byte[] data, String filename) {
        return ResponseEntity.ok().contentType(XLSX).header("Content-Disposition", "attachment; filename=" + filename).body(data);
    }
}