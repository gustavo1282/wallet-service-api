package com.guga.walletserviceapi.audit.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.guga.walletserviceapi.audit.*;

@Component
public class S3AuditEventPublisher implements AuditEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(S3AuditEventPublisher.class);
    public void publish(AuditEvent event) {
        log.debug("S3 audit stub: {}", event.getAction());
    }
}
