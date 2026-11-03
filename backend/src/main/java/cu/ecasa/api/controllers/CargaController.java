package cu.ecasa.api.controllers;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cu.ecasa.api.service.CargaService;

@RestController
@RequestMapping("/api/carga")
@PreAuthorize("hasAnyRole('Administrador', 'GrupoComunicaciones')")
public class CargaController {

    private static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private final CargaService service;

    public CargaController(CargaService service) { this.service = service; }

    @GetMapping("/plantilla")
    public ResponseEntity<byte[]> template() {
        return ResponseEntity.ok().contentType(XLSX).header("Content-Disposition", "attachment; filename=plantilla_carga_consumo.xlsx").body(service.template());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> upload(@RequestPart("archivo") MultipartFile archivo, @RequestParam int mes, @RequestParam int anio) {
        Map<String, Object> result = service.process(archivo, mes, anio);
        int status = ((java.util.List<?>) result.get("errores")).isEmpty() && ((Integer) result.get("duplicados")) == 0 ? 200 : 207;
        return ResponseEntity.status(status).body(result);
    }
}