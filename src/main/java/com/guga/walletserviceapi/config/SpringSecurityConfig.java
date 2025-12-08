package com.guga.walletserviceapi.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;

import com.guga.walletserviceapi.helpers.GlobalHelper;
import com.guga.walletserviceapi.security.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SpringSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
        JwtAuthenticationFilter jwtFilter) throws Exception {

        return http
                .csrf(csrf -> csrf.disable())

                .headers(headers -> headers
                    .frameOptions(frameOptions -> frameOptions.disable())
                )

                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    //config.setAllowedOrigins(Arrays.asList("http://localhost:8080", "http://seufrontend.com")); // << SUA ORIGEM AQUI
                    config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(Arrays.asList("*"));
                    config.setAllowCredentials(true);
                    return config;
                }))

                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers(publicMatchers()).permitAll()
                    .anyRequest().authenticated()
                )
                
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    private RequestMatcher[] publicMatchers() {
        List<String> matchers = GlobalHelper.matchers();

        return matchers.stream()
                .map(AntPathRequestMatcher::antMatcher)
                .toArray(RequestMatcher[]::new);
    }
}