package com.guga.walletserviceapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {SecurityAutoConfiguration.class})
//@ComponentScan(basePackages = "com.guga.walletserviceapi")
class WalletServiceApplicationTests {

    @Test
	void contextLoads() {
        System.out.println("============================================");
        System.out.println("WalletServiceApplicationTests > contextLoads()");
        System.out.println("============================================");
    }

}
