package com.guga.walletserviceapi;

import org.hibernate.cfg.Environment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
class WalletServiceApplicationTests {

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Test
	void contextLoads() {

        System.out.println("spring.profiles.active = ".concat(activeProfile) );

	}

}
