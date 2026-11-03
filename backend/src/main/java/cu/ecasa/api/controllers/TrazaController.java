package cu.ecasa.api.controllers;

import java.util.Map;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cu.ecasa.api.service.TrazaService;

@RestController
@RequestMapping("/api/trazas")
@PreAuthorize("hasRole('Administrador')")
public class TrazaController {

    private static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private final TrazaService service;

    public TrazaController(TrazaService service) { this.service = service; }

    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) Integer usuario_id, @RequestParam(required = false) String accion,
            @RequestParam(required = false) String entidad, @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta, @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "50") int limite) {
        return service.listar(usuario_id, accion, entidad, desde, hasta, pagina, limite);
    }

    @GetMapping("/exportar/excel")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) Integer usuario_id, @RequestParam(required = false) String accion,
            @RequestParam(required = false) String entidad, @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(XLSX);
        headers.setContentDisposition(ContentDisposition.attachment().filename("trazas_export.xlsx").build());
        return ResponseEntity.ok().headers(headers).body(service.exportar(usuario_id, accion, entidad, desde, hasta));
    }
}