package com.gugawallet.walletconsumer.consumer;

import java.time.LocalDateTime;

import com.gugawallet.walletconsumer.model.CustomerProcess;
import com.gugawallet.walletconsumer.model.CustomerRegisteredEvent;
import com.gugawallet.walletconsumer.model.CustomerStatus;
import com.gugawallet.walletconsumer.repository.CustomerProcessRepository;
import com.gugawallet.walletconsumer.service.ScorePolicyService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class CustomerRegistrationConsumer {

    private static final Logger logger = LoggerFactory.getLogger(CustomerRegistrationConsumer.class);

    private final CustomerProcessRepository repository;
    private final ScorePolicyService scorePolicyService;
    private final Counter processedCounter;
    private final Counter approvedCounter;
    private final Counter reviewCounter;
    private final Counter rejectedCounter;

    public CustomerRegistrationConsumer(CustomerProcessRepository repository,
                                        ScorePolicyService scorePolicyService,
                                        MeterRegistry meterRegistry) {
        this.repository = repository;
        this.scorePolicyService = scorePolicyService;
        this.processedCounter = meterRegistry.counter("onboarding.processed");
        this.approvedCounter = meterRegistry.counter("onboarding.approved");
        this.reviewCounter = meterRegistry.counter("onboarding.review");
        this.rejectedCounter = meterRegistry.counter("onboarding.rejected");
    }

    public void consume(CustomerRegisteredEvent event) {
        processedCounter.increment();
        logger.info("Processing customer registration: {}", event.getCustomerId());

        if (repository.findByCustomerId(event.getCustomerId())
            .filter(existing -> existing.getStatus() == CustomerStatus.ACTIVE
                || existing.getStatus() == CustomerStatus.REJECTED)
            .isPresent()) {
            logger.info("Customer {} already finalized. Skipping.", event.getCustomerId());
            return;
        }

        ScorePolicyService.ScoreDecision decision = scorePolicyService.evaluate(event.getScore());

        CustomerProcess process = repository.findByCustomerId(event.getCustomerId())
            .orElseGet(CustomerProcess::new);

        process.setCustomerId(event.getCustomerId());
        process.setStatus(decision.status());
        process.setScore(event.getScore());
        process.setProcessId(event.getProcessId());
        process.setUpdatedAt(LocalDateTime.now());

        repository.save(process);

        switch (decision.status()) {
            case ACTIVE -> approvedCounter.increment();
            case REVIEW -> reviewCounter.increment();
            case REJECTED -> rejectedCounter.increment();
            default -> {
            }
        }
    }
}
