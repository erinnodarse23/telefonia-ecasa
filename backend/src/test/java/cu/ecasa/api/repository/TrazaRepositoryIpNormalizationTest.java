package cu.ecasa.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests unitarios para la normalización de IPs en {@link TrazaRepository}.
 *
 * <p>Usa reflexión para acceder al método privado {@code normalizeIp}, que
 * es donde reside la lógica de limpieza. El resto del repositorio se
 * verifica con integración (no con unit tests).
 */
class TrazaRepositoryIpNormalizationTest {

    private Method normalizeIp;
    private TrazaRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        repository = new TrazaRepository(null);
        normalizeIp = TrazaRepository.class.getDeclaredMethod("normalizeIp", String.class);
        normalizeIp.setAccessible(true);
    }

    private String callNormalize(String input) throws Exception {
        return (String) normalizeIp.invoke(repository, input);
    }

    @Test
    @DisplayName("IP simple se mantiene")
    void ipSimple() throws Exception {
        assertThat(callNormalize("192.168.1.10")).isEqualTo("192.168.1.10");
        assertThat(callNormalize("127.0.0.1")).isEqualTo("127.0.0.1");
    }

    @Test
    @DisplayName("IPv4 con puerto se le quita el puerto")
    void ipConPuerto() throws Exception {
        assertThat(callNormalize("192.168.1.10:5432")).isEqualTo("192.168.1.10");
        assertThat(callNormalize("10.0.0.5:8080")).isEqualTo("10.0.0.5");
    }

    @Test
    @DisplayName("X-Forwarded-For con comas toma la primera IP")
    void ipConComas() throws Exception {
        assertThat(callNormalize("1.2.3.4, 10.0.0.1")).isEqualTo("1.2.3.4");
        assertThat(callNormalize("8.8.8.8, 1.1.1.1, 9.9.9.9")).isEqualTo("8.8.8.8");
        assertThat(callNormalize("1.2.3.4:5678, 10.0.0.1")).isEqualTo("1.2.3.4");
    }

    @Test
    @DisplayName("IPv6 se mantiene intacta")
    void ipv6() throws Exception {
        assertThat(callNormalize("::1")).isEqualTo("::1");
        assertThat(callNormalize("2001:db8::1")).isEqualTo("2001:db8::1");
    }

    @Test
    @DisplayName("null y vacío devuelven null")
    void nullYVacio() throws Exception {
        assertThat(callNormalize(null)).isNull();
        assertThat(callNormalize("")).isNull();
        assertThat(callNormalize("   ")).isNull();
    }

    @Test
    @DisplayName("valores especiales (unknown, null) devuelven null")
    void valoresEspeciales() throws Exception {
        assertThat(callNormalize("unknown")).isNull();
        assertThat(callNormalize("UNKNOWN")).isNull();
        assertThat(callNormalize("null")).isNull();
    }

    @Test
    @DisplayName("espacios alrededor se eliminan")
    void espacios() throws Exception {
        assertThat(callNormalize("  192.168.1.10  ")).isEqualTo("192.168.1.10");
    }
}