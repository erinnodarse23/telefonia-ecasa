package cu.ecasa.api.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cu.ecasa.api.repository.TrazaRepository;

/**
 * Tests unitarios para {@link TrazaService}.
 *
 * <p>Verifica la política fire-and-forget: registrar una traza nunca
 * propaga excepciones al llamador, pero sí loguea el error.
 */
@ExtendWith(MockitoExtension.class)
class TrazaServiceTest {

    @Mock
    private TrazaRepository trazaRepository;

    @InjectMocks
    private TrazaService trazaService;

    @Test
    @DisplayName("registrar() delega en el repositorio con los parámetros correctos")
    void registrar_delegaEnRepositorio() {
        trazaService.registrar(1, "CREATE", "EMPLEADO", 42, "Alta de empleado", "192.168.1.1");

        verify(trazaRepository, times(1)).save(
                eq(1), eq("CREATE"), eq("EMPLEADO"), eq(42),
                eq("Alta de empleado"), eq("192.168.1.1"));
    }

    @Test
    @DisplayName("registrar() no propaga excepción si el repositorio falla")
    void registrar_toleraFallo() {
        doThrow(new RuntimeException("IP inválida"))
                .when(trazaRepository).save(any(), any(), any(), any(), any(), any());

        // No debe lanzar: la operación de negocio ya fue exitosa
        trazaService.registrar(1, "CREATE", "EMPLEADO", 42, "Alta de empleado", "bad-ip");

        verify(trazaRepository, times(1)).save(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("registrar() acepta parámetros null")
    void registrar_aceptaNulls() {
        trazaService.registrar(null, "LOGIN", "USUARIO", null, null, null);

        verify(trazaRepository, times(1)).save(isNull(), eq("LOGIN"), eq("USUARIO"), isNull(), isNull(), isNull());
    }
}