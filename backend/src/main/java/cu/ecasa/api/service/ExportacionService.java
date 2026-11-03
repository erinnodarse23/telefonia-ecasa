package cu.ecasa.api.service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class ExportacionService {

    public byte[] excel(String sheetName, String[] headers, List<Map<String, Object>> rows) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);
            Row header = sheet.createRow(0);
            for (int index = 0; index < headers.length; index++) header.createCell(index).setCellValue(headers[index]);
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                Map<String, Object> data = rows.get(rowIndex);
                for (int column = 0; column < headers.length; column++) {
                    String key = keyFor(headers[column]);
                    Object value = data.get(key);
                    row.createCell(column).setCellValue(value == null ? "" : String.valueOf(value));
                }
            }
            for (int column = 0; column < headers.length; column++) sheet.autoSizeColumn(column);
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo generar el archivo Excel", exception);
        }
    }

    private String keyFor(String header) {
        return switch (header) {
            case "Empleado" -> "empleado_nombre";
            case "Cargo" -> "cargo";
            case "Teléfono" -> "empleado_telefono";
            case "Línea" -> "numero_linea";
            case "Marca" -> "marca";
            case "Modelo" -> "modelo";
            case "Fecha Asignación" -> "fecha_inicio";
            case "Número de Línea" -> "numero_linea";
            case "Estado Físico" -> "estado_fisico";
            case "Observaciones" -> "observaciones";
            case "UEB" -> "ueb_nombre";
            case "Servicios" -> "cantidad_servicios";
            case "% Datos" -> "pct_lineas_con_datos";
            case "Plan Anual" -> "plan_anual";
            case "Prom. Mensual" -> "promedio_real_mensual";
            case "Est. Cierre" -> "estimado_cierre";
            case "Diferencia" -> "diferencia";
            case "% Sobregasto" -> "pct_sobregasto";
            default -> header;
        };
    }
}