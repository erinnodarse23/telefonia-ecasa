package cu.ecasa.api.service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

@Service
public class PdfService {

    public byte[] responsables(List<Map<String, Object>> rows, String title) {
        return tablePdf(title, new String[]{"Empleado", "Cargo", "Telefono", "Linea", "Marca", "Modelo"}, rows,
                new String[]{"empleado_nombre", "cargo", "empleado_telefono", "numero_linea", "marca", "modelo"});
    }

    public byte[] equipos(List<Map<String, Object>> rows, String title) {
        return tablePdf(title, new String[]{"Numero", "Marca", "Modelo", "Estado", "Observaciones"}, rows,
                new String[]{"numero_linea", "marca", "modelo", "estado_fisico", "observaciones"});
    }

    /**
     * PDF del dashboard: tabla de indicadores por UEB.
     *
     * <p>Reutiliza el mismo formato de tabla que {@link #responsables} y
     * {@link #equipos}. Los valores se toman del {@code summary()} de
     * {@link DashboardService}, que ya calcula plan anual, promedios y
     * porcentaje de sobregasto.
     *
     * @param rows  filas del {@code dashboard.summary(anio, uebId)}
     * @param title título del PDF
     * @param anio  año (para el título secundario)
     * @return bytes del PDF
     */
    public byte[] dashboard(List<Map<String, Object>> rows, String title, Integer anio) {
        String fullTitle = title + (anio == null ? "" : " - " + anio);
        return tablePdf(fullTitle,
                new String[]{"UEB", "Servicios", "% Datos", "Plan Anual", "Prom. Mensual", "Est. Cierre", "% Sobregasto"},
                rows,
                new String[]{"ueb_nombre", "cantidad_servicios", "pct_lineas_con_datos",
                        "plan_anual", "promedio_real_mensual", "estimado_cierre", "pct_sobregasto"});
    }

    /**
     * PDF de trazas de auditoría.
     *
     * @param rows  filas del {@code TrazaService.listar(...).get("datos")}
     * @param title título del PDF
     * @return bytes del PDF
     */
    public byte[] trazas(List<Map<String, Object>> rows, String title) {
        return tablePdf(title,
                new String[]{"Fecha", "Usuario", "Rol", "Accion", "Entidad", "Descripcion", "IP"},
                rows,
                new String[]{"fecha", "usuario_login", "usuario_rol", "accion", "entidad", "descripcion", "ip"});
    }

    private byte[] tablePdf(String title, String[] headers, List<Map<String, Object>> rows, String[] keys) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, output);
            document.open();
            document.add(new Paragraph(title));
            PdfPTable table = new PdfPTable(headers.length);
            for (String header : headers) table.addCell(new Phrase(header));
            for (Map<String, Object> row : rows) {
                for (String key : keys) table.addCell(String.valueOf(row.getOrDefault(key, "")));
            }
            document.add(table);
            document.close();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo generar el PDF", exception);
        }
    }
}