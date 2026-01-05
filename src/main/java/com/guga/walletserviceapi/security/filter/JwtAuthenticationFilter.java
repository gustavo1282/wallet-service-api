package com.guga.walletserviceapi.security.filter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.guga.walletserviceapi.config.SecurityMatchers;
import com.guga.walletserviceapi.logging.LogMarkers;
import com.guga.walletserviceapi.security.JwtAuthenticationDetails;
import com.guga.walletserviceapi.security.jwt.JwtService;
import com.guga.walletserviceapi.service.LoginAuthService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LogManager.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtService jwtService;

    @Autowired
    @Lazy
    private LoginAuthService loginAuthService;

    @Autowired
    @Lazy
    private SecurityMatchers matchers;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {  

        
        // 1. Obter a URI COMPLETA da requisição (ex: /wallet-service-api/api/auth/login)
        String requestUri = request.getRequestURI(); 
        
        // 2. Obter o Context Path (ex: /wallet-service-api)
        String contextPath = request.getContextPath(); 
        
        LOGGER.info(LogMarkers.LOG, "JwtAutenticationFilter.shouldNotFilter [START]- URI={} ContextPath={}", 
            request.getRequestURI(), contextPath
        );
        
        // 3. Remover o Context Path para obter a URI 'limpa' (ex: /api/auth/login)
        // Isso garante que a comparação seja feita apenas com o caminho do recurso.
        String pathWithoutContext = requestUri.substring(contextPath.length());
        
        // Garante que o caminho seja pelo menos '/' se o contexto for a raiz
        if (pathWithoutContext.isEmpty()) {
            pathWithoutContext = "/";
        }


        // Cria uma versão da requisição que esconde o Context Path
        HttpServletRequest cleanedRequest = new HttpServletRequestWrapper(request) {
            @Override
            public String getRequestURI() {
                // Retorna apenas a URI após o Context Path
                return super.getRequestURI().substring(super.getContextPath().length());
            }

            @Override
            public String getContextPath() {
                // Retorna uma string vazia para o Context Path
                return "";
            }
        };

        // Agora o AntPathRequestMatcher irá receber a requisição "limpa" e fazer o match corretamente
        List<String> patterns = matchers.getAllMatchers();

        boolean isPublicUrl = patterns.stream()
            .anyMatch(pattern -> new AntPathRequestMatcher(pattern).matches(cleanedRequest));


        LOGGER.info(LogMarkers.LOG, "JwtAutenticationFilter.shouldNotFilter [SUCESS] - isPublicUrl={}", 
            isPublicUrl
        );

        // Se for uma URL pública, o filtro é ignorado.
        return isPublicUrl;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {


        LOGGER.info(LogMarkers.LOG, "JwtAutenticationFilter.doFilterInternal - validate shouldNotFilter");

        if (shouldNotFilter(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        LOGGER.info(LogMarkers.LOG, "JwtAutenticationFilter.doFilterInternal - validate Authorization Header");

        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        LOGGER.info(LogMarkers.LOG, "JwtAutenticationFilter.doFilterInternal - Jwt generated with success");

        if (!jwtService.validateToken(jwt)) {
            filterChain.doFilter(request, response);
            return;
        }

        // ===== Roles → Authorities =====
        List<String> roles = jwtService.extractRoles(jwt);

        List<GrantedAuthority> authorities = roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .collect(Collectors.toList())
            ;

        // ===== Identity (JWT Context) =====
        JwtAuthenticationDetails authDetails = JwtAuthenticationDetails.builder()
            .loginId(jwtService.extractLoginId(jwt))
            .login(jwtService.extractLogin(jwt))
            .customerId(jwtService.extractCustomerId(jwt))
            .walletId(jwtService.extractWalletId(jwt))
            .loginType(jwtService.extractLoginType(jwt))
            .roles(roles)
            .build();

        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(
                authDetails,
                null,
                authorities
            );

        authToken.setDetails(
            new WebAuthenticationDetailsSource().buildDetails(request)
        );

        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }


}