package com.guga.walletserviceapi.audit.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.guga.walletserviceapi.audit.*;

@Component
public class DbAuditEventPublisher implements AuditEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(DbAuditEventPublisher.class);
    public void publish(AuditEvent event) {
        log.debug("DB audit stub: {}", event.getAction());
    }
}
