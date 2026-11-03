package cu.ecasa.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import cu.ecasa.api.dto.request.UsuarioRequest;
import cu.ecasa.api.dto.request.UsuarioUpdateRequest;
import cu.ecasa.api.dto.response.UsuarioResponse;
import cu.ecasa.api.repository.UebRepository;
import cu.ecasa.api.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UebRepository uebRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TrazaService trazaService;

    @InjectMocks
    private UsuarioService usuarioService;

    private Map<String, Object> usuarioJefeUnidad;

    @BeforeEach
    void setUp() {
        usuarioJefeUnidad = new HashMap<>();
        usuarioJefeUnidad.put("id", 1);
        usuarioJefeUnidad.put("login", "jefe_nc");
        usuarioJefeUnidad.put("rol", "JefeUnidad");
        usuarioJefeUnidad.put("activo", true);
        usuarioJefeUnidad.put("ueb_id", 5);
    }

    @Test
    @DisplayName("update() cambio a Administrador limpia ueb_id y audita")
    void update_cambioRol_limpiaUebId() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuarioJefeUnidad));

        Map<String, Object> resultadoUpdate = new HashMap<>(usuarioJefeUnidad);
        resultadoUpdate.put("rol", "Administrador");
        resultadoUpdate.put("ueb_id", null);

        when(usuarioRepository.updateWithRoleAndUeb(
                eq(1), isNull(), isNull(), eq("Administrador"), isNull(), isNull()))
                .thenReturn(resultadoUpdate);

        UsuarioResponse response = usuarioService.update(1,
                new UsuarioUpdateRequest(null, null, "Administrador", null, null));

        verify(usuarioRepository, times(1)).updateWithRoleAndUeb(
                eq(1), isNull(), isNull(), eq("Administrador"), isNull(), isNull());
        verify(usuarioRepository, never()).update(any(), any(), any(), any(), any());
        verify(trazaService, times(1)).registrarAuto(
                eq("UPDATE"), eq("USUARIO"), eq(1), anyString());
        assertThat(response.rol()).isEqualTo("Administrador");
        assertThat(response.uebId()).isNull();
    }

    @Test
    @DisplayName("update() cambio a JefeUnidad asigna ueb_id y audita")
    void update_cambioRol_asignaUebId() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuarioJefeUnidad));
        when(uebRepository.findById(5)).thenReturn(Optional.of(Map.of("id", 5, "codigo", "NC")));

        Map<String, Object> resultadoUpdate = new HashMap<>(usuarioJefeUnidad);
        resultadoUpdate.put("rol", "JefeUnidad");
        resultadoUpdate.put("ueb_id", 5);

        when(usuarioRepository.updateWithRoleAndUeb(
                eq(1), isNull(), isNull(), eq("JefeUnidad"), isNull(), eq(5)))
                .thenReturn(resultadoUpdate);

        UsuarioResponse response = usuarioService.update(1,
                new UsuarioUpdateRequest(null, null, "JefeUnidad", null, 5));

        verify(usuarioRepository, times(1)).updateWithRoleAndUeb(
                eq(1), isNull(), isNull(), eq("JefeUnidad"), isNull(), eq(5));
        verify(trazaService, times(1)).registrarAuto(
                eq("UPDATE"), eq("USUARIO"), eq(1), anyString());
        assertThat(response.rol()).isEqualTo("JefeUnidad");
        assertThat(response.uebId()).isEqualTo(5);
    }

    @Test
    @DisplayName("update() sin cambio de rol NO toca ueb_id pero audita")
    void update_sinCambioRol_noTocaUebId() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuarioJefeUnidad));

        Map<String, Object> resultadoUpdate = new HashMap<>(usuarioJefeUnidad);
        resultadoUpdate.put("activo", false);

        when(usuarioRepository.update(eq(1), isNull(), isNull(), isNull(), eq(false)))
                .thenReturn(resultadoUpdate);

        usuarioService.update(1, new UsuarioUpdateRequest(null, null, null, false, null));

        verify(usuarioRepository, times(1)).update(eq(1), isNull(), isNull(), isNull(), eq(false));
        verify(usuarioRepository, never()).updateWithRoleAndUeb(any(), any(), any(), any(), any(), any());
        verify(trazaService, times(1)).registrarAuto(
                eq("UPDATE"), eq("USUARIO"), eq(1), anyString());
    }

    @Test
    @DisplayName("create() audita con acción CREATE")
    void create_audita() {
        when(usuarioRepository.findByLogin("nuevo")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed");

        Map<String, Object> resultadoCreate = new HashMap<>();
        resultadoCreate.put("id", 10);
        resultadoCreate.put("login", "nuevo");
        resultadoCreate.put("rol", "Administrador");
        resultadoCreate.put("activo", true);
        resultadoCreate.put("ueb_id", null);

        when(usuarioRepository.create(eq("nuevo"), eq("hashed"), eq("Administrador"), isNull()))
                .thenReturn(resultadoCreate);

        UsuarioResponse response = usuarioService.create(
                new UsuarioRequest("nuevo", "password123", "Administrador", null));

        verify(trazaService, times(1)).registrarAuto(
                eq("CREATE"), eq("USUARIO"), eq(10), anyString());
        assertThat(response.id()).isEqualTo(10);
        assertThat(response.login()).isEqualTo("nuevo");
    }

    @Test
    @DisplayName("delete() audita con acción DELETE")
    void delete_audita() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuarioJefeUnidad));

        usuarioService.delete(1);

        verify(usuarioRepository, times(1)).delete(1);
        verify(trazaService, times(1)).registrarAuto(
                eq("DELETE"), eq("USUARIO"), eq(1), anyString());
    }
        @Test
    @DisplayName("setActivo() audita y usa update parcial sin tocar ueb_id")
    void setActivo_auditaYUsaUpdateParcial() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuarioJefeUnidad));

        Map<String, Object> resultadoUpdate = new HashMap<>(usuarioJefeUnidad);
        resultadoUpdate.put("activo", true);

        when(usuarioRepository.update(eq(1), isNull(), isNull(), isNull(), eq(true)))
                .thenReturn(resultadoUpdate);

        usuarioService.setActivo(1, true);

        verify(usuarioRepository, times(1)).update(eq(1), isNull(), isNull(), isNull(), eq(true));
        verify(usuarioRepository, never()).updateWithRoleAndUeb(any(), any(), any(), any(), any(), any());
        verify(trazaService, times(1)).registrarAuto(
                eq("UPDATE"), eq("USUARIO"), eq(1), anyString());
    }
}