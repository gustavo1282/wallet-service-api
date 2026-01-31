package com.gugawallet.walletconsumer.config;

import java.util.function.Consumer;

import com.gugawallet.walletconsumer.consumer.CustomerRegistrationConsumer;
import com.gugawallet.walletconsumer.model.CustomerRegisteredEvent;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StreamConsumerConfig {

    @Bean
    public Consumer<CustomerRegisteredEvent> customerRegistration(CustomerRegistrationConsumer consumer) {
        return consumer::consume;
    }
}
