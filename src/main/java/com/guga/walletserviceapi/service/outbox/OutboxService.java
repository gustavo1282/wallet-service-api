package com.guga.walletserviceapi.service.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guga.walletserviceapi.model.Customer;
import com.guga.walletserviceapi.model.outbox.OutboxEvent;
import com.guga.walletserviceapi.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private static final String AGGREGATE_TYPE_CUSTOMER = "Customer";
    private static final String EVENT_CUSTOMER_REGISTERED = "customer.registration.v1";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxEvent registerCustomerRegistered(Customer customer) {
        CustomerRegisteredPayload payload = new CustomerRegisteredPayload(
            customer.getCustomerId(),
            customer.getDocumentId(),
            customer.getCpf(),
            customer.getFirstName(),
            customer.getLastName(),
            customer.getFullName(),
            customer.getEmail(),
            customer.getPhoneNumber(),
            customer.getStatus(),
            customer.getCreatedAt()
        );

        return outboxEventRepository.save(OutboxEvent.builder()
            .aggregateType(AGGREGATE_TYPE_CUSTOMER)
            .aggregateId(String.valueOf(customer.getCustomerId()))
            .eventType(EVENT_CUSTOMER_REGISTERED)
            .payload(toJson(payload))
            .build());
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        }
        catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize outbox payload", ex);
        }
    }
}
