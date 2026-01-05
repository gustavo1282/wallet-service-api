package com.guga.walletserviceapi.audit.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.guga.walletserviceapi.audit.*;

@Component
public class KafkaAuditEventPublisher implements AuditEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(KafkaAuditEventPublisher.class);
    public void publish(AuditEvent event) {
        log.debug("Kafka audit stub: {}", event.getAction());
    }
}
