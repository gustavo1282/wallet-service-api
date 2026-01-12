package com.guga.walletserviceapi.audit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.guga.walletserviceapi.logging.LogMarkers;


public class AuditLogger {

    private static final Logger LOGGER = LogManager.getLogger(AuditLogger.class);

    private AuditLogger() {}

    public static int log(String action, AuditLogContext ctx) {
        LOGGER.info(LogMarkers.AUDIT,
            "{} | user={} walletId={} traceId={} result={} ip={} info={}",
            action,
            ctx.getUsername(),
            ctx.getWalletId(),
            ctx.getTraceId(), 
            ctx.getResult(),
            ctx.getIpAddress(),
            ctx.getInfo()
        );
        return 0;
    }
    
}
