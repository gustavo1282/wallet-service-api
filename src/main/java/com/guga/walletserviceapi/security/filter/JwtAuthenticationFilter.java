package com.guga.walletserviceapi.security.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.guga.walletserviceapi.config.SecurityMatchers;
import com.guga.walletserviceapi.logging.LogMarkers;
import com.guga.walletserviceapi.model.enums.LoginRole;
import com.guga.walletserviceapi.security.JwtAuthenticationDetails;
import com.guga.walletserviceapi.security.jwt.JwtService;
import com.guga.walletserviceapi.service.LoginAuthService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LogManager.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;

    @Lazy
    private final LoginAuthService loginAuthService;

    @Lazy
    private final SecurityMatchers matchers;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Value("${app.api-prefix:}")
    private String servletPath;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 1) Rotas permitAll/documentação/infra: pula JWT
        if (shouldSkipJwt(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2) Extrai Authorization
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
            // Aqui é endpoint "protegido" (não skipamos), então faz sentido avisar
            LOGGER.warn(LogMarkers.LOG, "JwtAuthFilter - missing Authorization Bearer");
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        LOGGER.debug(LogMarkers.LOG, "JwtAuthFilter - JWT received (len={})", jwt.length());

        // 3) Valida token
        if (!jwtService.validateToken(jwt)) {
            LOGGER.warn(LogMarkers.LOG, "JwtAuthFilter - invalid JWT");
            filterChain.doFilter(request, response);
            return;
        }

        // 4) Roles → Authorities
        List<LoginRole> roles = jwtService.extractRoles(jwt).stream()
            .filter(Objects::nonNull)
            .map(role -> LoginRole.valueOf(role.trim().toUpperCase()))
            .toList();

        List<GrantedAuthority> authorities = roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .collect(Collectors.toList());

        // 5) Identity (JWT Context)
        JwtAuthenticationDetails authDetails = JwtAuthenticationDetails.builder()
            .loginId(jwtService.extractLoginId(jwt))
            .login(jwtService.extractLogin(jwt))
            .customerId(jwtService.extractCustomerId(jwt))
            .walletId(jwtService.extractWalletId(jwt))
            .loginType(jwtService.extractLoginType(jwt))
            .roles(roles)
            .build();

        // 6) Set authentication context
        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(authDetails, null, authorities);

        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }

    private List<String> getMatchersSession(String[] value) {
        if (value == null || value.length == 0) return List.of();
        return Arrays.asList(value);
    }

    /**
     * Pula o JWT apenas para rotas que são permitAll (públicas/documentação/infra).
     * Para rotas secured/admin, o filtro DEVE rodar.
     */
    private boolean shouldSkipJwt(HttpServletRequest request) {

        String path = request.getRequestURI()
            .replace(contextPath, "")
            .replace(servletPath, "");

        // Infra
        if (path.startsWith("/actuator/prometheus")) {
            return true;
        }

        List<String> publicPaths = getMatchersSession(matchers.getPublicPaths());
        List<String> documentationPaths = getMatchersSession(matchers.getDocumentation());
        List<String> securedPaths = getMatchersSession(matchers.getSecured());
        List<String> adminPaths = getMatchersSession(matchers.getAdmin());

        AntPathMatcher pathMatcher = new AntPathMatcher();

        boolean isPublic = publicPaths.stream()
            .filter(Objects::nonNull)
            .anyMatch(p -> pathMatcher.match(p, path));

        boolean isDocumentation = documentationPaths.stream()
            .filter(Objects::nonNull)
            .anyMatch(p -> pathMatcher.match(p, path));

        boolean isSecured = securedPaths.stream()
            .filter(Objects::nonNull)
            .anyMatch(p -> pathMatcher.match(p, path));

        boolean isAdmin = adminPaths.stream()
            .filter(Objects::nonNull)
            .anyMatch(p -> pathMatcher.match(p, path));

        return (isPublic || isDocumentation || isSecured || isAdmin);
    }
}