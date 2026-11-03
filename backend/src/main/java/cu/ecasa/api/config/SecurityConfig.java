package cu.ecasa.api.config;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import cu.ecasa.api.exceptions.ApiErrorWriter;
import cu.ecasa.api.security.JwtAuthenticationFilter;

/**
 * Configuración de seguridad de la API.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private static final long CORS_MAX_AGE_SECONDS = 3600L;

    private static final List<String> CORS_ALLOWED_METHODS =
            List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD");

    private static final List<String> CORS_EXPOSED_HEADERS =
            List.of("Content-Disposition", "Authorization");

    /**
     * Rutas públicas. Incluye TODAS las variantes de Swagger/Springdoc.
     */
    private static final String[] PUBLIC_ENDPOINTS = {
            // Health checks
            "/api/health",
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
            // Login y captcha
            "/api/auth/login",
            "/api/usuarios/login",
            "/api/usuarios/captcha",
            // Swagger UI (Springdoc) — todas las variantes
            "/swagger-ui.html",
            "/swagger-ui",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/webjars/**",
            // Ruta custom antigua (por si acaso)
            "/api/docs",
            "/api/docs/**"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final String corsOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          @Value("${app.cors-origin}") String corsOrigins) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.corsOrigins = corsOrigins;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated()
                )
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                ApiErrorWriter.write(response, 401, "No autorizado"))
                        .accessDeniedHandler((request, response, exception) ->
                                ApiErrorWriter.write(response, 403, "Acceso denegado"))
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> origins = Arrays.stream(corsOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        config.setAllowedOrigins(origins);

        config.setAllowedMethods(CORS_ALLOWED_METHODS);
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(CORS_EXPOSED_HEADERS);
        config.setAllowCredentials(false);
        config.setMaxAge(CORS_MAX_AGE_SECONDS);

        log.info("CORS configurado. Orígenes permitidos: {}", origins);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}