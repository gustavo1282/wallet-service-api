package com.guga.walletserviceapi.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;



@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(SeedController.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED) // Garante que o Seed persista no banco
//@SpringBootTest
@Import({
    org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration.class,
    com.guga.walletserviceapi.config.ConfigProperties.class,
    com.guga.walletserviceapi.seeder.SeedRunner.class, 
    com.guga.walletserviceapi.seeder.SeedExecutor.class, 
    com.guga.walletserviceapi.seeder.SeedOrderConfig.class,
    com.guga.walletserviceapi.config.ApiDocsTags.class
})
class SeedControllerTest extends BaseControllerTest {

    @MockitoBean
    private com.guga.walletserviceapi.config.ApiDocsResponses apiDocsTags;

    @Test
    void whenRunSeeder_thenReturnsOk() throws Exception {

        bearerAuth = null;
        performRequest(HttpMethod.POST, "/seeder/admin/run", null, null)
            .andExpect(status().isOk())
            .andExpect(content().string("Seeding process executed successfully."));
            ;
    }

    @Override
    public void loadMockData() {
       
    }
}
