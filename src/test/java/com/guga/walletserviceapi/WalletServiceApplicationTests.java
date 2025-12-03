package com.guga.walletserviceapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@EnableAutoConfiguration(exclude = {SecurityAutoConfiguration.class})
@ActiveProfiles("test")
@WithMockUser(username = "user", roles = {"USER"})
class WalletServiceApplicationTests {

    @Test
	void contextLoads() {
        System.out.println("============================================");
        System.out.println("WalletServiceApplicationTests > contextLoads()");
        System.out.println("============================================");
    }

}
