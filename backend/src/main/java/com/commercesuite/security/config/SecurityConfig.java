package com.commercesuite.security.config;

import com.commercesuite.common.api.ApiResponse;
import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.security.filter.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(com.commercesuite.security.jwt.JwtProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, ObjectMapper mapper) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST,
                        "/api/v1/auth/signup",
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/password/forgot",
                        "/api/v1/auth/password/reset",
                        "/api/v1/auth/email/verify").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info",
                        "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/catalog/**").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(eh -> eh
                .authenticationEntryPoint((req, res, ex) -> write(mapper, res, HttpServletResponse.SC_UNAUTHORIZED,
                        ErrorCode.UNAUTHENTICATED, "Authentication required"))
                .accessDeniedHandler((req, res, ex) -> write(mapper, res, HttpServletResponse.SC_FORBIDDEN,
                        ErrorCode.FORBIDDEN, "Access denied")))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setExposedHeaders(List.of("X-Request-Id"));
        cfg.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    private static void write(ObjectMapper m, HttpServletResponse res, int status, ErrorCode code, String msg) throws java.io.IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        var body = new ApiResponse<>(false,
                new com.commercesuite.common.exception.GlobalExceptionHandler.ErrorBody(code.name(), msg, java.util.Map.of()),
                msg, java.time.Instant.now());
        res.getWriter().write(m.writeValueAsString(body));
    }
}
