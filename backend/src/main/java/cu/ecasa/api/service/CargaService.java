package cu.ecasa.api.service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import cu.ecasa.api.repository.ConsumoRepository;
import cu.ecasa.api.repository.EquipoRepository;
import cu.ecasa.api.util.ExcelValueParser;

/**
 * Servicio de carga masiva de consumos desde archivos Excel.
 *
 * <p><b>Atomicidad:</b> {@link #process} es transaccional para garantizar que
 * los múltiples {@code INSERT consumo} y la reevaluación de alertas se
 * ejecuten como una unidad. Si algo falla a mitad, se revierte todo.
 *
 * <p><b>Parsing robusto:</b> usa {@link ExcelValueParser} para manejar
 * formato US ({@code 1,234.56}) y español ({@code 1.234,56}).
 *
 * @see ConsumoRepository
 * @see AlertGenerationService
 * @see ExcelValueParser
 */
@Service
public class CargaService {

    private static final Logger log = LoggerFactory.getLogger(CargaService.class);

    private final ConsumoRepository consumoRepository;
    private final EquipoRepository equipoRepository;
    private final AlertGenerationService alertGenerationService;

    public CargaService(ConsumoRepository consumoRepository,
                        EquipoRepository equipoRepository,
                        AlertGenerationService alertGenerationService) {
        this.consumoRepository = consumoRepository;
        this.equipoRepository = equipoRepository;
        this.alertGenerationService = alertGenerationService;
    }

    /**
     * Procesa el archivo Excel de consumos para un período (año, mes).
     */
    @Transactional
    public Map<String, Object> process(MultipartFile file, int mes, int anio) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No se recibió ningún archivo");
        }
        if (mes < 1 || mes > 12 || anio < 2020 || anio > 2030) {
            throw new IllegalArgumentException(
                    "Los parámetros mes (1-12) y año (2020-2030) son obligatorios y válidos");
        }

        List<Map<String, Object>> errors = new ArrayList<>();
        int processed = 0;
        int duplicates = 0;

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> columns = headers(sheet.getRow(0));
            DataFormatter formatter = new DataFormatter();

            for (int index = 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null) continue;
                int line = index + 1;
                String numero = value(row, columns, "numero_linea", formatter);

                if (numero == null || numero.isBlank()) {
                    errors.add(error(line, "Número de línea no especificado"));
                    continue;
                }

                var equipo = equipoRepository.findByNumero(numero.trim());
                if (equipo.isEmpty()) {
                    errors.add(error(line, "Número de línea " + numero + " no encontrado"));
                    continue;
                }

                try {
                    BigDecimal voz = ExcelValueParser.parseDecimal(
                            value(row, columns, "voz_consumido", formatter));
                    BigDecimal datos = ExcelValueParser.parseDecimal(
                            value(row, columns, "datos_consumido", formatter));
                    Integer sms = ExcelValueParser.parseInteger(
                            value(row, columns, "sms_consumido", formatter));

                    int inserted = consumoRepository.insertIfAbsent(
                            ((Number) equipo.get().get("id")).intValue(),
                            anio, mes, voz, datos, sms);

                    if (inserted == 0) duplicates++;
                    else processed++;
                } catch (IllegalArgumentException exception) {
                    errors.add(error(line, exception.getMessage()));
                }
            }
        } catch (Exception exception) {
            throw new IllegalArgumentException("No se pudo leer el archivo Excel", exception);
        }

        reevaluarAlertasTrasCarga();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mensaje", errors.isEmpty() && duplicates == 0
                ? "Carga exitosa"
                : "Carga completada con problemas");
        result.put("procesados", processed);
        result.put("errores", errors);
        result.put("duplicados", duplicates);
        return result;
    }

    /**
     * Plantilla Excel vacía con las columnas esperadas.
     */
    public byte[] template() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Consumos");
            Row headers = sheet.createRow(0);
            String[] values = {"numero_linea", "voz_consumido", "datos_consumido", "sms_consumido"};
            for (int i = 0; i < values.length; i++) {
                headers.createCell(i).setCellValue(values[i]);
            }

            workbook.write(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo generar la plantilla", exception);
        }
    }

    private void reevaluarAlertasTrasCarga() {
        try {
            alertGenerationService.evaluarTodasLasUebs();
        } catch (Exception ex) {
            log.warn("Reevaluación de alertas tras carga falló: {}", ex.getMessage());
        }
    }

    private Map<String, Integer> headers(Row row) {
        Map<String, Integer> headers = new LinkedHashMap<>();
        if (row == null) throw new IllegalArgumentException("El archivo no contiene encabezados");
        for (int i = 0; i < row.getLastCellNum(); i++) {
            headers.put(row.getCell(i).getStringCellValue().trim(), i);
        }
        return headers;
    }

    private String value(Row row, Map<String, Integer> columns, String column, DataFormatter formatter) {
        Integer index = columns.get(column);
        return index == null || row.getCell(index) == null
                ? ""
                : formatter.formatCellValue(row.getCell(index)).trim();
    }

    private Map<String, Object> error(int line, String message) {
        return Map.of("fila", line, "error", message);
    }
}