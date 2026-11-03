package cu.ecasa.api.service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import cu.ecasa.api.dto.request.UsuarioRequest;
import cu.ecasa.api.dto.request.AsignacionRequest;
import cu.ecasa.api.dto.request.EmpleadoRequest;
import cu.ecasa.api.dto.request.UebRequest;
import cu.ecasa.api.dto.request.EquipoRequest;
import cu.ecasa.api.dto.request.PlanRequest;
import cu.ecasa.api.repository.EmpleadoRepository;
import cu.ecasa.api.repository.EquipoRepository;
import cu.ecasa.api.repository.UebRepository;
import cu.ecasa.api.util.ExcelValueParser;

/**
 * Servicio de importación masiva de datos desde archivos Excel.
 *
 * <p>Soporta las tablas {@code ueb}, {@code empleado}, {@code equipo},
 * {@code asignacion}, {@code plan} y {@code usuario}. Para cada fila válida
 * delega en el servicio correspondiente (que aplica sus propias validaciones
 * de negocio). Los errores de validación se acumulan por fila para devolver
 * un 207 Multi-Status con el detalle.
 *
 * <p><b>Parsing robusto:</b> usa {@link ExcelValueParser} para manejar
 * formato US ({@code 1,234.56}) y español ({@code 1.234,56}) y fechas en
 * múltiples formatos (ISO, EU, US).
 *
 * @see ExcelValueParser
 */
@Service
public class ImportacionService {

    private static final Map<String, String[]> COLUMNS = Map.of(
            "ueb", new String[]{"codigo", "nombre", "descripcion"},
            "empleado", new String[]{"nombre", "cargo", "departamento", "telefono", "ueb_codigo"},
            "equipo", new String[]{"numero_linea", "marca", "modelo", "estado_fisico", "observaciones"},
            "asignacion", new String[]{"numero_linea", "empleado_id", "fecha_inicio", "estado_equipo_entrega"},
            "plan", new String[]{"numero_linea", "voz_plan", "datos_plan", "sms_plan", "cuota", "fecha_inicio"},
            "usuario", new String[]{"login", "password", "rol", "ueb_id"});

    private final UebService uebService;
    private final EmpleadoService empleadoService;
    private final EquipoService equipoService;
    private final AsignacionService asignacionService;
    private final PlanService planService;
    private final UsuarioService usuarioService;
    private final UebRepository uebRepository;
    private final EquipoRepository equipoRepository;
    private final EmpleadoRepository empleadoRepository;

    public ImportacionService(UebService uebService, EmpleadoService empleadoService, EquipoService equipoService,
                              AsignacionService asignacionService, PlanService planService, UsuarioService usuarioService,
                              UebRepository uebRepository, EquipoRepository equipoRepository, EmpleadoRepository empleadoRepository) {
        this.uebService = uebService;
        this.empleadoService = empleadoService;
        this.equipoService = equipoService;
        this.asignacionService = asignacionService;
        this.planService = planService;
        this.usuarioService = usuarioService;
        this.uebRepository = uebRepository;
        this.equipoRepository = equipoRepository;
        this.empleadoRepository = empleadoRepository;
    }

    /**
     * Genera la plantilla Excel vacía con las columnas esperadas para una tabla.
     */
    public byte[] template(String table) {
        String[] columns = requireTable(table);
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(table);
            Row row = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                row.createCell(i).setCellValue(columns[i]);
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo generar la plantilla", exception);
        }
    }

    /**
     * Importa un archivo Excel para la tabla indicada.
     */
    public Map<String, Object> importFile(String table, MultipartFile file) {
        requireTable(table);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No se recibió ningún archivo");
        }

        List<Map<String, Object>> errors = new ArrayList<>();
        int processed = 0;

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            Map<String, Integer> columns = headers(sheet.getRow(0));

            for (int index = 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null) continue;
                int line = index + 1;
                try {
                    importRow(table, row, columns, formatter);
                    processed++;
                } catch (Exception exception) {
                    errors.add(Map.of("fila", line, "error", message(exception)));
                }
            }
        } catch (Exception exception) {
            throw new IllegalArgumentException("No se pudo leer el archivo Excel", exception);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mensaje", errors.isEmpty()
                ? "Carga exitosa: " + processed + " registro(s)"
                : "Carga completada con errores");
        result.put("procesados", processed);
        result.put("errores", errors);
        return result;
    }

    private void importRow(String table, Row row, Map<String, Integer> columns, DataFormatter formatter) {
        switch (table) {
            case "ueb" -> uebService.create(new UebRequest(
        value(row, columns, "codigo", formatter),
        value(row, columns, "nombre", formatter),
        value(row, columns, "descripcion", formatter)));

           case "empleado" -> empleadoService.create(new EmpleadoRequest(
        value(row, columns, "nombre", formatter),
        value(row, columns, "cargo", formatter),
        value(row, columns, "departamento", formatter),
        value(row, columns, "telefono", formatter),
        uebId(value(row, columns, "ueb_codigo", formatter))));

            case "equipo" -> equipoService.create(new EquipoRequest(
        value(row, columns, "numero_linea", formatter),
        value(row, columns, "marca", formatter),
        value(row, columns, "modelo", formatter),
        value(row, columns, "estado_fisico", formatter),
        value(row, columns, "observaciones", formatter)));

           case "plan" -> {
    var equipo = equipoRepository.findByNumero(value(row, columns, "numero_linea", formatter))
            .orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado"));
    planService.create(new PlanRequest(
            ((Number) equipo.get("id")).intValue(),
            ExcelValueParser.parseInteger(value(row, columns, "voz_plan", formatter)),
            ExcelValueParser.parseInteger(value(row, columns, "datos_plan", formatter)),
            ExcelValueParser.parseInteger(value(row, columns, "sms_plan", formatter)),
            ExcelValueParser.parseDecimal(value(row, columns, "cuota", formatter)),
            ExcelValueParser.parseDate(value(row, columns, "fecha_inicio", formatter))));
}

           case "asignacion" -> {
    var equipo = equipoRepository.findByNumero(value(row, columns, "numero_linea", formatter))
            .orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado"));
    asignacionService.create(new AsignacionRequest(
            ((Number) equipo.get("id")).intValue(),
            ExcelValueParser.parseInteger(value(row, columns, "empleado_id", formatter)),
            ExcelValueParser.parseDate(value(row, columns, "fecha_inicio", formatter)),
            value(row, columns, "estado_equipo_entrega", formatter)));
}


            // ✅ FIX: ahora UsuarioService.create() recibe UsuarioRequest, no strings
            case "usuario" -> usuarioService.create(new UsuarioRequest(
                    value(row, columns, "login", formatter),
                    value(row, columns, "password", formatter),
                    value(row, columns, "rol", formatter),
                    ExcelValueParser.parseInteger(value(row, columns, "ueb_id", formatter))));

            default -> throw new IllegalArgumentException("Tabla no soportada");
        }
    }

    private Integer uebId(String codigo) {
        return uebRepository.findByCodigo(codigo)
                .map(row -> ((Number) row.get("id")).intValue())
                .orElseThrow(() -> new IllegalArgumentException("UEB no encontrada"));
    }

    private String[] requireTable(String table) {
        String[] columns = COLUMNS.get(table);
        if (columns == null) {
            throw new IllegalArgumentException("Tabla " + table + " no soportada");
        }
        return columns;
    }

    private Map<String, Integer> headers(Row row) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (row == null) throw new IllegalArgumentException("El archivo no contiene encabezados");
        for (int i = 0; i < row.getLastCellNum(); i++) {
            result.put(row.getCell(i).getStringCellValue().trim(), i);
        }
        return result;
    }

    private String value(Row row, Map<String, Integer> columns, String key, DataFormatter formatter) {
        Integer i = columns.get(key);
        return i == null || row.getCell(i) == null
                ? ""
                : formatter.formatCellValue(row.getCell(i)).trim();
    }

    private String message(Exception exception) {
        return exception.getMessage() == null ? "Error de validación" : exception.getMessage();
    }
}