package com.guga.walletserviceapi.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import com.guga.walletserviceapi.config.web.TraceIdFilter;
import com.guga.walletserviceapi.security.filter.JwtAuthenticationFilter;
import com.guga.walletserviceapi.security.handler.CustomAccessDeniedHandler;
import com.guga.walletserviceapi.security.handler.CustomAuthenticationEntryPoint;

import lombok.RequiredArgsConstructor;

@Configuration
//@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final TraceIdFilter traceIdFilter;

    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

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
                config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
                config.setAllowedHeaders(Arrays.asList("*"));
                config.setAllowCredentials(false);
                return config;
            }))

            .sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            .headers(headers -> headers.frameOptions(f -> f.sameOrigin()))

            // =========================
            // Tratamento de erros (401/403)
            // =========================
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(customAuthenticationEntryPoint)
                .accessDeniedHandler(customAccessDeniedHandler)
            )

            // =========================
            // Autorização
            // =========================
            .authorizeHttpRequests(auth -> auth

                // H2 Console (Servlet) -> use Ant matcher
                .requestMatchers(new AntPathRequestMatcher("/h2-console/**")).permitAll()

                // 1) Público primeiro
                .requestMatchers(mvcMatchers(mvc, matchers.getPublicPaths())).permitAll()
                .requestMatchers(mvcMatchers(mvc, matchers.getDocumentation())).permitAll()

                // 2) Negócio (USER e ADMIN)
                .requestMatchers(mvcMatchers(mvc, matchers.getSecured())).hasAnyRole("USER", "ADMIN")

                // 3) Restrito (ADMIN)
                .requestMatchers(mvcMatchers(mvc, matchers.getAdmin())).hasRole("ADMIN")

                // 4) Qualquer outra coisa: bloqueia
                .anyRequest().denyAll()
            )

            // =========================
            // Filters (ordem IMPORTANTE)
            // TraceId primeiro, depois JWT
            // =========================
            .addFilterBefore(traceIdFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(jwtAuthenticationFilter, TraceIdFilter.class)

            .build();
    }

    // -----------------------------------------------------
    // Helper: converte String[] em array de matchers MVC
    // -----------------------------------------------------
    private MvcRequestMatcher[] mvcMatchers(MvcRequestMatcher.Builder mvc, String[] patterns) {
        if (patterns == null || patterns.length == 0) return new MvcRequestMatcher[0];

        MvcRequestMatcher[] matchers = new MvcRequestMatcher[patterns.length];
        for (int i = 0; i < patterns.length; i++) {
            matchers[i] = mvc.pattern(patterns[i]);
        }
        return matchers;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}