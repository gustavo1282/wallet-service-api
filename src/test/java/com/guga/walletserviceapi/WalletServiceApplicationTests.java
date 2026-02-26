package com.guga.walletserviceapi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import com.guga.walletserviceapi.controller.BaseControllerTest;
import com.guga.walletserviceapi.helpers.GlobalHelper;
import com.guga.walletserviceapi.logging.LogMarkers;


@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "MANAGEMENT_OTLP_ENDPOINT=http://localhost:4317",
    }
)
@AutoConfigureMockMvc     // [somente quando @SpringBootTest] injeta e cria todos os beans
//@WebMvcTest     // [Somente em ControllerTests - sem @SpringBootTest] trabalha no modelo web slice (mais rápido) só de controllers
class WalletServiceApplicationTests extends BaseControllerTest {

    private static final Logger LOGGER = LogManager.getLogger(WalletServiceApplicationTests.class);

    @Test
	void contextLoads() {
        LOGGER.info(LogMarkers.LOG, "============================================");
        LOGGER.info(LogMarkers.LOG, "WalletServiceApplicationTests > contextLoads()");
        LOGGER.info(LogMarkers.LOG, "============================================");

        GlobalHelper.printAllVariables(env, LOGGER);

    }

    @Override
    public void loadMockData() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'loadMockData'");
    }
    
}
