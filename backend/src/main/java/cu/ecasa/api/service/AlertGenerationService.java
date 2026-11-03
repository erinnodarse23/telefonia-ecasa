package cu.ecasa.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cu.ecasa.api.repository.AlertaRepository;
import cu.ecasa.api.repository.DashboardRepository;

/**
 * Servicio de generación automática de alertas de sobregasto.
 *
 * <p>Cada 6 horas (configurable vía {@code app.alerts.evaluation-cron}) evalúa
 * todas las UEBs activas con umbral configurado y genera alertas cuando el
 * consumo acumulado del año supera el umbral definido por UEB.
 *
 * <p>Lógica por UEB:
 * <ol>
 *   <li>Obtiene el {@code plan_anual.monto} del año actual</li>
 *   <li>Si el plan es {@code 0} o {@code null}, omite la evaluación</li>
 *   <li>Calcula el consumo acumulado del año (voz + datos)</li>
 *   <li>Calcula el porcentaje: {@code (consumo / plan) * 100}</li>
 *   <li>Si {@code porcentaje >= umbral} y no existe alerta para el período
 *       actual ({@code año-mes}), inserta una nueva alerta</li>
 * </ol>
 *
 * <p>La idempotencia se garantiza con {@code existsByPeriodo()} y la constraint
 * {@code UNIQUE (ueb_id, anio, mes, tipo)} de la tabla {@code alerta}.
 *
 * @see AlertaRepository
 * @see DashboardRepository
 */
@Service
public class AlertGenerationService {

    private static final Logger log = LoggerFactory.getLogger(AlertGenerationService.class);

    /** Tipo de alerta generada por este servicio. */
    private static final String TIPO_ALERTA = "SOBREGASTO";

    /** Multiplicador para convertir fracción a porcentaje. */
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private static final String[] NOMBRES_MES = {
            "enero", "febrero", "marzo", "abril", "mayo", "junio",
            "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
    };

    private final AlertaRepository alertaRepository;
    private final DashboardRepository dashboardRepository;

    public AlertGenerationService(AlertaRepository alertaRepository,
                                  DashboardRepository dashboardRepository) {
        this.alertaRepository = alertaRepository;
        this.dashboardRepository = dashboardRepository;
    }

    /**
     * Job programado de evaluación de umbrales por UEB.
     *
     * <p>Por defecto se ejecuta cada 6 horas. Configurable con la propiedad
     * {@code app.alerts.evaluation-cron}. Puede invocarse manualmente desde
     * {@link cu.ecasa.api.service.CargaService} o {@link cu.ecasa.api.service.ConsumoService}
     * tras importar/actualizar consumos para reevaluar inmediatamente.
     */
    @Scheduled(cron = "${app.alerts.evaluation-cron:0 0 */6 * * *}")
    @Transactional
    public void evaluarTodasLasUebs() {
        YearMonth periodo = YearMonth.now();
        int anio = periodo.getYear();
        int mes = periodo.getMonthValue();

        List<Map<String, Object>> uebs = alertaRepository.findUebsConUmbral();

        if (uebs.isEmpty()) {
            log.debug("AlertGenerationService: no hay UEBs con umbral configurado");
            return;
        }

        log.debug("AlertGenerationService: evaluando {} UEB(s) para período {}-{}",
                uebs.size(), anio, mes);

        int generadas = 0;

        for (Map<String, Object> ueb : uebs) {
            try {
                if (evaluarUeb(ueb, anio, mes)) {
                    generadas++;
                }
            } catch (Exception ex) {
                // No romper todo el ciclo por una UEB concreta
                log.error("AlertGenerationService: error evaluando UEB {}", ueb.get("codigo"), ex);
            }
        }

        if (generadas > 0) {
            log.info("AlertGenerationService: {} alerta(s) generada(s) para período {}-{}",
                    generadas, anio, mes);
        } else {
            log.debug("AlertGenerationService: ninguna UEB superó su umbral en {}-{}",
                    anio, mes);
        }
    }

    /**
     * Evalúa una UEB concreta y genera alerta si corresponde.
     *
     * @return {@code true} si se generó una nueva alerta
     */
    private boolean evaluarUeb(Map<String, Object> ueb, int anio, int mes) {
        Integer uebId = ((Number) ueb.get("id")).intValue();
        String codigo = String.valueOf(ueb.get("codigo"));
        String nombre = String.valueOf(ueb.get("nombre"));
        BigDecimal umbral = toBigDecimal(ueb.get("umbral_sobregasto"));

        if (umbral == null || umbral.signum() <= 0) {
            return false;
        }

        BigDecimal planAnual = obtenerPlanAnual(uebId, anio);
        if (planAnual == null || planAnual.signum() == 0) {
            log.debug("AlertGenerationService: UEB {} sin plan anual para {} — omitida",
                    codigo, anio);
            return false;
        }

        BigDecimal consumo = alertaRepository.calcularConsumoAcumuladoPorUeb(uebId, anio);
        if (consumo.signum() == 0) {
            return false;
        }

        BigDecimal porcentaje = consumo
                .multiply(CIEN)
                .divide(planAnual, 2, RoundingMode.HALF_UP);

        if (porcentaje.compareTo(umbral) < 0) {
            return false;
        }

        if (alertaRepository.existsByPeriodo(uebId, anio, mes, TIPO_ALERTA)) {
            log.debug("AlertGenerationService: alerta ya existe para UEB {} en {}-{} — omitida",
                    codigo, anio, mes);
            return false;
        }

        String mensaje = construirMensaje(nombre, codigo, porcentaje, umbral, planAnual, anio, mes);

        alertaRepository.insert(uebId, anio, mes, TIPO_ALERTA, porcentaje, mensaje);
        log.info("AlertGenerationService: alerta generada para UEB {} — {}% consumido (umbral {}%)",
                codigo, porcentaje, umbral);

        return true;
    }

    private BigDecimal obtenerPlanAnual(Integer uebId, int anio) {
        Map<String, Object> row = dashboardRepository.findAnnualPlan(uebId, anio);
        if (row == null) {
            return null;
        }
        return toBigDecimal(row.get("monto"));
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return new BigDecimal(value.toString());
    }

    private String construirMensaje(String nombre, String codigo, BigDecimal porcentaje,
                                    BigDecimal umbral, BigDecimal planAnual, int anio, int mes) {
        return String.format(
                "La UEB %s (%s) ha consumido el %.2f%% de su plan anual %d ($%s), "
                        + "superando el umbral configurado del %d%%. Período: %s %d.",
                nombre, codigo, porcentaje, anio,
                planAnual.setScale(2, RoundingMode.HALF_UP),
                umbral.intValue(),
                NOMBRES_MES[mes - 1], anio);
    }
}