package com.guga.walletserviceapi.config.web;

import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class TraceInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) {
        
        // ----
        // TRACE_ID
        String traceId = getTraceId(request);
        
        org.apache.logging.log4j.ThreadContext.put("traceId", traceId);

        // ----
        // GEOLOCALIZACAO - REGION BY IP
        // 2. Localização (Estratégia 1: Ordem de prioridade dos Headers)
        String region = getRegionFromHeaders(request);
        ThreadContext.put("region", region);

        return true;
    }

    private String getTraceId(HttpServletRequest httpServletRequest) {
        // 1. Tenta pegar do ThreadContext (já populado pelo Interceptor)
        String trace = ThreadContext.get("traceId");
        
        // 2. Fallback: Se nulo (ex: erro antes do Interceptor), tenta o Header
        if (trace == null && httpServletRequest  != null) {
            trace = httpServletRequest.getHeader("X-Trace-Id");
        }
        
        // 3. Último recurso: Gera um novo para não ficar nulo
        return (trace != null) ? trace : UUID.randomUUID().toString();
    }

    private String getRegionFromHeaders(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("CF-IPCountry")) // Cloudflare
            .or(() -> Optional.ofNullable(request.getHeader("CloudFront-Viewer-Country-Region"))) // AWS
            .or(() -> Optional.ofNullable(request.getHeader("X-App-Region"))) // Nosso Mock de Teste
            .orElse("UNKNOWN"); // Fallback final
    }

    @Override
    public void afterCompletion(HttpServletRequest request, 
                                HttpServletResponse response, 
                                Object handler, 
                                Exception ex) {
        org.apache.logging.log4j.ThreadContext.clearAll();
    }
}
