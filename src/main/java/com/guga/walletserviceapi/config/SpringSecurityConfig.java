package com.guga.walletserviceapi.config;

import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.security.web.util.matcher.RequestMatcher;
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

    private static final Logger LOGGER = LogManager.getLogger(SpringSecurityConfig.class);

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final SecurityMatchers matchers;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Value("${app.api-prefix:}")
    private String servletPath;

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

            .headers(headers -> headers.frameOptions(f -> f.sameOrigin()))

            // =========================
            // Autorização
            // =========================
            .authorizeHttpRequests(auth -> auth
    
                // 1. Libera o que é Público primeiro (Regra mais genérica)
                .requestMatchers(mvcMatchers(mvc, matchers.getPublicPaths())).permitAll()
                .requestMatchers(mvcMatchers(mvc, matchers.getDocumentation())).permitAll()

                // 2. NEGÓCIO (USER e ADMIN)
                // hasAnyRole já valida que está autenticado E tem o perfil
                .requestMatchers(mvcMatchers(mvc, matchers.getSecured())).hasAnyRole("USER", "ADMIN")

                // 3. RESTRITO (Apenas ADMIN)
                // Aqui entrará o /actuator/metrics e o resto do /api/v1/params/**
                .requestMatchers(mvcMatchers(mvc, matchers.getAdmin())).hasRole("ADMIN")

                // 4. BLOQUEIO TOTAL
                // Tudo que não foi listado acima é sumariamente negado
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
        return paths;
    }

    private RequestMatcher[] mvcMatchers(MvcRequestMatcher.Builder mvc, String[] paths) {
        if (paths == null) return new RequestMatcher[0];
        return Arrays.stream(clean(paths))
            .map(mvc::pattern)
            .toArray(RequestMatcher[]::new);
    }

}
