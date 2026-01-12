package com.guga.walletserviceapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class ConfigProperties {

    @Bean
    @ConfigurationProperties(prefix = "security.access-levels")
    public SecurityMatchers securityMatchers(Environment env) {
        SecurityMatchers matchers = new SecurityMatchers();
        // Garantir que o contextPath seja setado a partir das propriedades do Spring
        String contextPath = env.getProperty("server.servlet.context-path");
        if (contextPath == null || contextPath.trim().isEmpty()) {
            contextPath = "/";
        }
        matchers.setContextPath(contextPath);
        return matchers;
    }
}