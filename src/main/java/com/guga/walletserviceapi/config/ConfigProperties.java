package com.guga.walletserviceapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfigProperties {

    @Bean
    @ConfigurationProperties(prefix = "security.access-levels")
    public SecurityMatchers securityMatchers() {
        return new SecurityMatchers();
    }
}