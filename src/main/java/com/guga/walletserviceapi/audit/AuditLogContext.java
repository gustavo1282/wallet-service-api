package com.guga.walletserviceapi.audit;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.guga.walletserviceapi.helpers.GlobalHelper;
import com.guga.walletserviceapi.model.LoginAuth;
import com.guga.walletserviceapi.security.JwtAuthenticationDetails;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@JsonPropertyOrder({
    "traceId",
    "sessionId",
    "sequenceId",
    "username",
    "userAgent",
    "ipAddress",
    "action",
    "resource",
    "result",
    "info",
    "timeMillis",
    "createAt"
})
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class AuditLogContext {

    /** ===== Identidade / rastreabilidade ===== */
    private String traceId;
    private String sessionId;
    private Long sequenceId;

    /** ===== Usuário ===== */
    private String username;

    /** ===== Domínio Keys ===== */
    private final Long loginId;
    private final Long customerId;
    private final Long walletId;
    private final String loginType;

    /** ===== Request ===== */
    private String userAgent;
    private final String ipAddress;

    /** ===== Ação auditada ===== */
    private String action;
    private String resource;
    private String result;
    private String info;
    private Long timeMillis;

    /** ===== Timestamp ===== */
    @JsonFormat(
        shape = JsonFormat.Shape.STRING,
        pattern = GlobalHelper.PATTERN_FORMAT_DATE_TIME
    )
    @Builder.Default
    private LocalDateTime createAt = LocalDateTime.now();

    /** ===== Defaults ===== */
    @Builder.Default
    private Long sequenceIdDefault = LocalDateTime.now()
        .atZone(ZoneId.of("America/Sao_Paulo"))
        .toInstant()
        .toEpochMilli();



    public static AuditLogContext from(
        JwtAuthenticationDetails details
    ) {

        return builderLogContext(
            details.getLogin(),
            details.getLoginId(),
            details.getCustomerId(),
            details.getWalletId(),
            details.getLoginType()
        );        
    }        


    /**
     * Factory method OFICIAL para criar AuditLogContext
     * a partir do JWT autenticado.
     */
    public static AuditLogContext from(
        LoginAuth loginAuth
    ) {

        return builderLogContext(
            loginAuth.getLogin(),
            loginAuth.getId(),
            loginAuth.getCustomerId(),
            loginAuth.getWalletId(),
            loginAuth.getLoginAuthType().name()
        );        
    }

    private static AuditLogContext builderLogContext(String login, Long loginId, Long customerId, Long walletId, String loginType) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    
        String ipAddress = null;
        String userAgent = null;
        String traceId = null;

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();

            ipAddress = request.getRemoteAddr();
            userAgent = request.getHeader("User-Agent");
            traceId = ThreadContext.get("traceId");
        }

        Long sequenceIdDefault = LocalDateTime.now()
                    .atZone(ZoneId.of("America/Sao_Paulo"))
                    .toInstant()
                    .toEpochMilli();

        return AuditLogContext.builder()
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .traceId(traceId)
            .username(login)
            .loginId(loginId)
            .customerId(customerId)
            .walletId(walletId)
            .loginType(loginType)
            .sequenceId(sequenceIdDefault)
            .timeMillis(Instant.now().toEpochMilli())
            .build();
    }

    @Override
    public String toString() {
        return "AuditLogContext(" +
            "traceId=" + traceId +
            ", username=" + username +
            
            ", loginId=" + loginId +
            ", customerId=" + customerId +
            ", walletId=" + walletId +
            ", loginType=" + loginType +

            ", action=" + action +
            ", resource=" + resource +
            ", result=" + result +
            ", ipAddress=" + ipAddress +
            ", createAt=" + createAt +
            ')';
    }
}
