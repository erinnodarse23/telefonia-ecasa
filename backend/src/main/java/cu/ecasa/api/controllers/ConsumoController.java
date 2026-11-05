package cu.ecasa.api.controllers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cu.ecasa.api.dto.request.ConsumoCreateRequest;
import cu.ecasa.api.service.ConsumoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/consumo")
@PreAuthorize("hasAnyRole('Administrador', 'GrupoComunicaciones')")
public class ConsumoController {

    private final ConsumoService service;

    public ConsumoController(ConsumoService service) {
        this.service = service;
    }

    @GetMapping("/equipo/{equipoId}")
    public List<Map<String, Object>> findByEquipo(@PathVariable Integer equipoId) {
        return service.findByEquipo(equipoId);
    }

    /**
     * Crea un consumo manual.
     *
     * <p>Devuelve {@code 201 Created} con la fila creada.
     * Si ya existe un consumo para {@code (equipoId, anio, mes)},
     * devuelve {@code 409 Conflict}.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody ConsumoCreateRequest request) {
        Map<String, Object> result = service.create(
                request.equipoId(),
                request.anio(),
                request.mes(),
                request.vozConsumido(),
                request.datosConsumido(),
                request.smsConsumido());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Integer id, @RequestBody ConsumoRequest request) {
        return service.update(id, request.voz_consumido(), request.datos_consumido(), request.sms_consumido());
    }

    public record ConsumoRequest(BigDecimal voz_consumido, BigDecimal datos_consumido, Integer sms_consumido) {
    }
}