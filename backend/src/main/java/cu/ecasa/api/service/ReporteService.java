package cu.ecasa.api.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import cu.ecasa.api.repository.ReporteRepository;

@Service
public class ReporteService {

    private final ReporteRepository repository;

    public ReporteService(ReporteRepository repository) { this.repository = repository; }

    public List<Map<String, Object>> responsables(Integer uebId) { return repository.responsables(uebId); }

    public List<Map<String, Object>> equiposPorEstado(String estado, Integer uebId) {
        if (estado != null && !java.util.Set.of("nuevo", "bueno", "dañado", "de baja").contains(estado)) {
            throw new IllegalArgumentException("Estado inválido");
        }
        return repository.equiposPorEstado(estado, uebId);
    }
}