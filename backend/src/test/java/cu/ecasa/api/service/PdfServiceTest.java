package cu.ecasa.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PdfServiceTest {

    private PdfService pdf;

    @BeforeEach
    void setUp() {
        pdf = new PdfService();
    }

    @Test
    @DisplayName("responsables genera un PDF no vacío")
    void responsables_generaPdf() {
        Map<String, Object> row = Map.of(
                "empleado_nombre", "Carlos Mendoza",
                "cargo", "Jefe",
                "empleado_telefono", "555-1234",
                "numero_linea", "53001001",
                "marca", "Samsung",
                "modelo", "A52");
        byte[] data = pdf.responsables(List.of(row), "Test");
        assertThat(data).isNotEmpty();
        // Cabecera %PDF-
        assertThat(new String(data, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    @DisplayName("dashboard genera un PDF no vacío con el año en el título")
    void dashboard_generaPdf() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("ueb_nombre", "Nivel Central");
        row.put("cantidad_servicios", 100);
        row.put("pct_lineas_con_datos", 80);
        row.put("plan_anual", new BigDecimal("62400"));
        row.put("promedio_real_mensual", new BigDecimal("5200"));
        row.put("estimado_cierre", new BigDecimal("62400"));
        row.put("pct_sobregasto", 0);

        byte[] data = pdf.dashboard(List.of(row), "Dashboard", 2025);
        assertThat(data).isNotEmpty();
        assertThat(new String(data, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    @DisplayName("trazas genera un PDF no vacío")
    void trazas_generaPdf() {
        Map<String, Object> row = Map.of(
                "fecha", "2026-11-04 10:00:00",
                "usuario_login", "admin",
                "usuario_rol", "Administrador",
                "accion", "CREATE",
                "entidad", "EQUIPO",
                "descripcion", "Creó equipo",
                "ip", "1.2.3.4");
        byte[] data = pdf.trazas(List.of(row), "Trazas");
        assertThat(data).isNotEmpty();
        assertThat(new String(data, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    @DisplayName("lista vacía genera PDF válido (con cabeceras)")
    void listaVacia_generaPdf() {
        byte[] data = pdf.trazas(List.of(), "Trazas vacías");
        assertThat(data).isNotEmpty();
        assertThat(new String(data, 0, 4)).isEqualTo("%PDF");
    }
}