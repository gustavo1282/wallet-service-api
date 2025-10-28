package com.guga.walletserviceapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {SecurityAutoConfiguration.class})
class WalletServiceApplicationTests {

    //@Value("${spring.profiles.active:default}")
    private String activeProfile;

    private String BASE_PATH;

    @Test
	void contextLoads() {
//        System.out.println("============================================");
//        System.out.println("WalletServiceApplicationTests > contextLoads()");
//
//        System.out.println("spring.profiles.active = ".concat(activeProfile) );
//        System.out.println("BASE_PATH = ".concat(BASE_PATH) );
//
//        System.out.println("============================================");
    }

}
