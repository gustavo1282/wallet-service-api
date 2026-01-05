package com.guga.walletserviceapi.audit.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.guga.walletserviceapi.audit.*;

@Component
public class ElasticAuditEventPublisher implements AuditEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(ElasticAuditEventPublisher.class);
    public void publish(AuditEvent event) {
        log.debug("Elastic audit stub: {}", event.getAction());
    }
}
