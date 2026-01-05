package com.guga.walletserviceapi.audit;

public interface AuditEventPublisher {
    void publish(AuditEvent event);
}
