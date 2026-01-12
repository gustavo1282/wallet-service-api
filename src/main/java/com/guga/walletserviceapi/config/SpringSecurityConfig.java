package com.guga.walletserviceapi.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import com.guga.walletserviceapi.security.filter.JwtAuthenticationFilter;
import com.guga.walletserviceapi.security.handler.CustomAccessDeniedHandler;
import com.guga.walletserviceapi.security.handler.CustomAuthenticationEntryPoint;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SpringSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final SecurityMatchers matchers;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, HandlerMappingIntrospector introspector) throws Exception {

        MvcRequestMatcher.Builder mvc = new MvcRequestMatcher.Builder(introspector);    

        return http
            // =========================
            // Segurança básica
            // =========================
            .csrf(csrf -> csrf.disable())

            .cors(cors -> cors.configurationSource(request -> {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOrigins(Arrays.asList("*"));
                config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(Arrays.asList("*"));
                config.setAllowCredentials(false);
                return config;
            }))

            .sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // =========================
            // Autorização
            // =========================
            .authorizeHttpRequests(auth -> auth
                // 1. Endpoints de Infraestrutura (Sempre permitidos)
                .requestMatchers(mvc.pattern("/actuator/**")).permitAll()
                .requestMatchers(mvc.pattern("/v3/api-docs/**")).permitAll()
                .requestMatchers(mvc.pattern("/swagger-ui/**")).permitAll()
                .requestMatchers(mvc.pattern("/swagger-ui.html")).permitAll()
                .requestMatchers(mvc.pattern("/webjars/**")).permitAll()

                // 2. Regras Dinâmicas do YAML (Sanitizadas para remover o context-path se existir)
                .requestMatchers(clean(matchers.getPublicPaths())).permitAll()
                .requestMatchers(clean(matchers.getDocumentation())).permitAll()
                .requestMatchers(clean(matchers.getSecured())).authenticated()
                
                // 3. Bloqueio residual
                .anyRequest().denyAll()
            )

            // =========================
            // JWT Filter
            // =========================
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

            // =========================
            // Tratamento de erros
            // =========================
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )

            .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Remove o context-path dos matchers para garantir compatibilidade com o 
     * processamento interno do Spring Security.
     */
    private String[] clean(String[] paths) {
        if (paths == null) return new String[0];
        return Arrays.stream(paths)
                .map(p -> p.replace("/wallet-service-api", ""))
                .toArray(String[]::new);
    }

}
