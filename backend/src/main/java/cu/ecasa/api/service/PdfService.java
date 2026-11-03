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