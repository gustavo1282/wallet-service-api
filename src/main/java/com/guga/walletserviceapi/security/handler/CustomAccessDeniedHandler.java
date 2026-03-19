package com.guga.walletserviceapi.security.handler;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guga.walletserviceapi.handler.ErrorResponse;
import com.guga.walletserviceapi.model.enums.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public CustomAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {

        String traceId = Optional.ofNullable(ThreadContext.get("traceId"))
                .orElse(UUID.randomUUID().toString());

        response.setHeader("X-Trace-Id", traceId);

        HttpStatus status = HttpStatus.FORBIDDEN;

        String message = "Access denied.";

        String path = resolvePath(request);

        String source = "security.authorization";

        ErrorResponse body = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                ErrorCode.FORBIDDEN.getCode(), // ex: SECURITY_403
                message,
                source,
                path,
                traceId,
                Instant.now()
        );

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private String resolvePath(HttpServletRequest request) {
        if (request == null) return null;
        String method = request.getMethod();
        String uri = request.getRequestURI();
        return method + " " + uri;
    }

}