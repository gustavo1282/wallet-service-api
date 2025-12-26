package com.guga.walletserviceapi.config;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.guga.walletserviceapi.model.dto.AuditLogContext;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class AuditContextFactory {

    public AuditLogContext from(HttpServletRequest request) {

        Authentication authentication =
            SecurityContextHolder.getContext().getAuthentication();

        return AuditLogContext.builder()
            .sessionId(request.getSession().getId())
            .userAgent(request.getHeader("User-Agent"))
            .ipAddress(request.getRemoteAddr())
            .username(authentication != null ? authentication.getName() : "anonymous")
            .traceId(ThreadContext.get("traceId"))
            .build();
    }
}