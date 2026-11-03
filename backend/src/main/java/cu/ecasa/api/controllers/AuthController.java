package cu.ecasa.api.controllers;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cu.ecasa.api.dto.request.LoginRequest;
import cu.ecasa.api.dto.response.CaptchaResponse;
import cu.ecasa.api.dto.response.LoginResponse;
import cu.ecasa.api.dto.response.MessageResponse;
import cu.ecasa.api.dto.response.VerifyResponse;
import cu.ecasa.api.security.AuthenticatedUser;
import cu.ecasa.api.service.AuthService;

import jakarta.validation.Valid;

/**
 * Controlador de autenticación.
 *
 * <p>Los endpoints de login, captcha y verify usan DTOs tipados para
 * que Swagger documente los esquemas correctamente.
 */
@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping({"/usuarios/login", "/auth/login"})
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/usuarios/captcha")
    public CaptchaResponse captcha() {
        return authService.generateCaptcha();
    }

    @GetMapping("/auth/verify")
    public VerifyResponse verify(Authentication authentication) {
        return authService.verify(
                ((AuthenticatedUser) authentication.getPrincipal()).id());
    }

    @PostMapping("/auth/logout")
    public MessageResponse logout(@RequestHeader("Authorization") String authorization) {
        authService.logout(authorization.substring("Bearer ".length()).trim());
        return new MessageResponse("Sesión cerrada correctamente");
    }
}