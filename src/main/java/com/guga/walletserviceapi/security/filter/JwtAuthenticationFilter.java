package com.guga.walletserviceapi.security.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Value("${app.api-prefix:}")
    private String servletPath;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // final String path = Optional.ofNullable(request.getRequestURI())
        //     .map(p -> contextPath == null ? p : p.replaceAll(contextPath, ""))
        //     //.map(p ->  servletPath == null ? p : p.replaceAll(servletPath, ""))
        //     .orElse("");

        // Libera se for público ou documentação
        String[] publicArr = matchers.getPublicPaths();
        String[] docArr = matchers.getDocumentation();
        String[] allPathsPermited = matchers.getPermitAllPaths();

        List<String> publicPaths = publicArr == null ? List.of() : Arrays.asList(publicArr);
        List<String> documentationPaths = docArr == null ? List.of() : Arrays.asList(docArr);
        List<String> allPaths = allPathsPermited == null ? List.of() : Arrays.asList(allPathsPermited);

        AntPathMatcher pathMatcher = new AntPathMatcher();

        boolean isPublic = publicPaths.stream().filter(Objects::nonNull).anyMatch(p -> pathMatcher.match(p, path));
        boolean isDocumentation = documentationPaths.stream().filter(Objects::nonNull).anyMatch(p -> pathMatcher.match(p, path));
        boolean isPermitAll = allPaths.stream().filter(Objects::nonNull).anyMatch(p -> pathMatcher.match(p, path));
        
        if (isPublic || isDocumentation || isPermitAll) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
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
        List<LoginRole> roles = jwtService.extractRoles(jwt).stream()
            .filter(Objects::nonNull)
            .map(role -> LoginRole.valueOf(role.trim().toUpperCase()))
            .toList();

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