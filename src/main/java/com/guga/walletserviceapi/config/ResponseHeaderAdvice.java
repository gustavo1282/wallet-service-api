package com.guga.walletserviceapi.config;

import java.util.UUID;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestControllerAdvice
public class ResponseHeaderAdvice implements ResponseBodyAdvice<Object> {

    //// O Spring injeta o proxy da requisição atual aqui
    //private final HttpServletRequest httpServletRequest;

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        return true; // Aplica em todos os endpoints @RestController
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                  MediaType selectedContentType, Class selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {

        // // Recupera o traceId que o Interceptor colocou no Log4j/ThreadContext
        // String traceId = ThreadContext.get("traceId");
        String traceId = getTraceId();
        if (traceId != null) {
            // Use .set em vez de .add para evitar a UnsupportedOperationException
            response.getHeaders().set("X-Trace-Id", traceId);
        }

        return body;
    }

    private String getTraceId() {
        // 1. Tenta pegar do ThreadContext (já populado pelo Interceptor)
        String trace = ThreadContext.get("traceId");

        // // 2. Fallback: Se nulo (ex: erro antes do Interceptor), tenta o Header
        // if (trace == null && httpServletRequest  != null) {
        //     trace = httpServletRequest.getHeader("X-Trace-Id");
        // }

        // // 3. Último recurso: Gera um novo para não ficar nulo
        return (trace != null) ? trace : UUID.randomUUID().toString();
    }

}
