package com.guga.walletserviceapi.security;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.guga.walletserviceapi.config.SecurityMatchers;
import com.guga.walletserviceapi.service.LoginAuthService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

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

        // Se for uma URL pública, o filtro é ignorado.
        return isPublicUrl;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getServletPath();

        if (shouldNotFilter(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        if (jwtService.validateToken(jwt)) {
            String username = jwtService.extractUsername(jwt);

            UserDetails userDetails  = loginAuthService.loadUserByUsername(username);

            UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }
}