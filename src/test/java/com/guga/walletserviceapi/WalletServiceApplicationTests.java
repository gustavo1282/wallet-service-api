package com.guga.walletserviceapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import com.guga.walletserviceapi.controller.BaseControllerTest;
import com.guga.walletserviceapi.helpers.GlobalHelper;


@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc     // [somente quando @SpringBootTest] injeta e cria todos os beans
//@WebMvcTest     // [Somente em ControllerTests - sem @SpringBootTest] trabalha no modelo web slice (mais rápido) só de controllers
class WalletServiceApplicationTests extends BaseControllerTest {

    @Test
	void contextLoads() {
        System.out.println("============================================");
        System.out.println("WalletServiceApplicationTests > contextLoads()");
        System.out.println("============================================");

        GlobalHelper.printAllVariables(env, LOGGER);

    }

    @Override
    public void loadMockData() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'loadMockData'");
    }
    
}
