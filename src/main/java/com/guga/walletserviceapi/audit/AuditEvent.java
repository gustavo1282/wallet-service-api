package com.guga.walletserviceapi.audit;

import java.time.Instant;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuditEvent {
    private final String action;
    private final String username;
    private final Long customerId;
    private final Long walletId;
    private final String ipAddress;
    private final String traceId;
    private final Instant timestamp;

    public static AuditEvent from(String action, AuditLogContext ctx) {
        return AuditEvent.builder()
            .action(action)
            .username(ctx.getUsername())
            .customerId(ctx.getCustomerId())
            .walletId(ctx.getWalletId())
            .ipAddress(ctx.getIpAddress())
            .traceId(ctx.getTraceId())
            //.timestamp(ctx.getCreateAt())
            .build();
    }
}
