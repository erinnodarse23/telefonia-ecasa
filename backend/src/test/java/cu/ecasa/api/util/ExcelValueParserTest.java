package cu.ecasa.api.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests unitarios para {@link ExcelValueParser}.
 *
 * <p>Cubre los formatos de decimal y fecha más comunes producidos por Excel
 * en configuraciones regionales US y españolas.
 */
class ExcelValueParserTest {

    // ============================================================
    // parseDecimal
    // ============================================================

    @Test
    @DisplayName("parseDecimal: formato US sin miles")
    void parseDecimal_usSinMiles() {
        assertThat(ExcelValueParser.parseDecimal("1234.56")).isEqualByComparingTo(new BigDecimal("1234.56"));
    }

    @Test
    @DisplayName("parseDecimal: formato EU sin miles")
    void parseDecimal_euSinMiles() {
        assertThat(ExcelValueParser.parseDecimal("1234,56")).isEqualByComparingTo(new BigDecimal("1234.56"));
    }

    @Test
    @DisplayName("parseDecimal: formato US con miles")
    void parseDecimal_usConMiles() {
        assertThat(ExcelValueParser.parseDecimal("1,234.56")).isEqualByComparingTo(new BigDecimal("1234.56"));
    }

    @Test
    @DisplayName("parseDecimal: formato EU con miles")
    void parseDecimal_euConMiles() {
        assertThat(ExcelValueParser.parseDecimal("1.234,56")).isEqualByComparingTo(new BigDecimal("1234.56"));
    }

    @Test
    @DisplayName("parseDecimal: entero sin decimales")
    void parseDecimal_entero() {
        assertThat(ExcelValueParser.parseDecimal("1234")).isEqualByComparingTo(new BigDecimal("1234"));
    }

    @Test
    @DisplayName("parseDecimal: miles US sin decimales (1,234 → 1234)")
    void parseDecimal_milesUs() {
        assertThat(ExcelValueParser.parseDecimal("1,234")).isEqualByComparingTo(new BigDecimal("1234"));
    }

    @Test
    @DisplayName("parseDecimal: null y vacío devuelven ZERO")
    void parseDecimal_vacio() {
        assertThat(ExcelValueParser.parseDecimal(null)).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(ExcelValueParser.parseDecimal("")).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(ExcelValueParser.parseDecimal("   ")).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("parseDecimal: valor no numérico lanza excepción clara")
    void parseDecimal_invalido() {
        assertThatThrownBy(() -> ExcelValueParser.parseDecimal("abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no numérico")
                .hasMessageContaining("abc");
    }

    // ============================================================
    // parseInteger
    // ============================================================

    @Test
    @DisplayName("parseInteger: valor simple")
    void parseInteger_simple() {
        assertThat(ExcelValueParser.parseInteger("100")).isEqualTo(100);
    }

    @Test
    @DisplayName("parseInteger: con miles")
    void parseInteger_conMiles() {
        assertThat(ExcelValueParser.parseInteger("1,500")).isEqualTo(1500);
    }

    @Test
    @DisplayName("parseInteger: null y vacío devuelven 0")
    void parseInteger_vacio() {
        assertThat(ExcelValueParser.parseInteger(null)).isZero();
        assertThat(ExcelValueParser.parseInteger("")).isZero();
    }

    @Test
    @DisplayName("parseInteger: valor con decimales lanza excepción")
    void parseInteger_conDecimales() {
        assertThatThrownBy(() -> ExcelValueParser.parseInteger("100.5"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entero");
    }

    // ============================================================
    // parseDate
    // ============================================================

    @Test
    @DisplayName("parseDate: formato ISO")
    void parseDate_iso() {
        assertThat(ExcelValueParser.parseDate("2025-01-15")).isEqualTo(LocalDate.of(2025, 1, 15));
    }

    @Test
    @DisplayName("parseDate: formato EU dd/MM/yyyy")
    void parseDate_eu() {
        assertThat(ExcelValueParser.parseDate("15/01/2025")).isEqualTo(LocalDate.of(2025, 1, 15));
    }

    @Test
    @DisplayName("parseDate: formato EU con guion")
    void parseDate_euGuion() {
        assertThat(ExcelValueParser.parseDate("15-01-2025")).isEqualTo(LocalDate.of(2025, 1, 15));
    }

    @Test
    @DisplayName("parseDate: null y vacío devuelven null")
    void parseDate_vacio() {
        assertThat(ExcelValueParser.parseDate(null)).isNull();
        assertThat(ExcelValueParser.parseDate("")).isNull();
    }

    @Test
    @DisplayName("parseDate: fecha inválida lanza excepción clara")
    void parseDate_invalida() {
        assertThatThrownBy(() -> ExcelValueParser.parseDate("no-es-fecha"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Fecha no válida");
    }
}