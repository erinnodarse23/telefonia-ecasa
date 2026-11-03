package cu.ecasa.api.controllers;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cu.ecasa.api.service.ImportacionService;

@RestController
@RequestMapping("/api/importacion")
@PreAuthorize("hasAnyRole('Administrador', 'GrupoComunicaciones')")
public class ImportacionController {

    private static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private final ImportacionService service;

    public ImportacionController(ImportacionService service) { this.service = service; }

    @GetMapping("/plantilla/{tabla}")
    public ResponseEntity<byte[]> template(@PathVariable String tabla) {
        return ResponseEntity.ok().contentType(XLSX).header("Content-Disposition", "attachment; filename=plantilla_" + tabla + ".xlsx").body(service.template(tabla));
    }

    @PostMapping(value = "/cargar/{tabla}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> upload(@PathVariable String tabla, @RequestPart("archivo") MultipartFile archivo) {
        Map<String, Object> result = service.importFile(tabla, archivo);
        int status = ((java.util.List<?>) result.get("errores")).isEmpty() ? 200 : 207;
        return ResponseEntity.status(status).body(result);
    }
}