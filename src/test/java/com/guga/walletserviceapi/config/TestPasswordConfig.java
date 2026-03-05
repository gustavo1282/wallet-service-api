package com.guga.walletserviceapi.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

//@Profile("test")
@TestConfiguration
public class TestPasswordConfig {
    @Bean
    //@Primary
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(4);
    }
}
