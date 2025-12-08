package com.guga.walletserviceapi.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.guga.walletserviceapi.security.JwtService;


@ActiveProfiles("test")
@WithMockUser(username = "user", roles = {"USER"})
@WebMvcTest(AuthController.class)
@Import({AuthControllerTests.TestConfig.class})
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;
    
    @Autowired // Este campo agora está acessível para os métodos de teste
    private PasswordEncoder passwordEncoder; 

    @Value("${controller.path.base}")
    private String BASE_PATH;

    private static final String API_NAME = "/auth";

    // A anotação @TestConfiguration carrega esta classe no contexto do teste
    @TestConfiguration 
    static class TestConfig {
        
        // Beans que serão mockados
        @Bean
        public PasswordEncoder passwordEncoder() {
            return mock(PasswordEncoder.class);
        }

        @Bean
        public JwtService jwtService() {
            return mock(JwtService.class);
        }
    }

    private String URI_API;
    private static String LOGIN_USER = "l_guga";
    private static String LOGIN_PASSWORD = "p_ruts";

    private String json;

    @BeforeEach
    void setup() {
        // Configuramos os mocks autowired aqui:
        when(jwtService.generateAccessToken(LOGIN_USER)).thenReturn(LOGIN_PASSWORD);
        when(jwtService.generateRefreshToken(LOGIN_USER)).thenReturn(LOGIN_PASSWORD);
        
        // Configuramos o mock do PasswordEncoder aqui:
        when(passwordEncoder.encode(LOGIN_PASSWORD)).thenReturn("encoded123");


        json = String.format("""
            {
              "username": "%s",
              "password": "%s"
            }
            """, LOGIN_USER, LOGIN_PASSWORD);


        URI_API = BASE_PATH.concat(API_NAME);
    }

    @Test
    void testLoginReturnsTokens() throws Exception {
        mockMvc.perform(post(URI_API.concat("/login"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andDo(result -> System.out.println("Status: " + result.getResponse().getStatus()))
                .andDo(result -> System.out
                                .println("Body: " + result.getResponse().getContentAsString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(LOGIN_PASSWORD))
                .andExpect(jsonPath("$.refreshToken").value(LOGIN_PASSWORD));
    }

    @Test
    void testRegisterReturnsEncodedPassword() throws Exception {
        // A linha "when(passwordEncoder.encode(LOGIN_PASSWORD)).thenReturn("encoded123");"
        // foi movida para o BeforeEach.
        
        mockMvc.perform(post(URI_API.concat("/register"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encodedPassword").value("encoded123"));
    }

    @Test
    void testRefreshGeneratesNewAccessToken() throws Exception {
        when(jwtService.validateToken(LOGIN_PASSWORD)).thenReturn(true);
        when(jwtService.extractUsername(LOGIN_PASSWORD)).thenReturn(LOGIN_USER);
        when(jwtService.generateAccessToken(LOGIN_USER)).thenReturn("newAccessToken");

        mockMvc.perform(post(URI_API.concat("/refresh"))
                .param("refreshToken", LOGIN_PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("newAccessToken"))
                .andExpect(jsonPath("$.refreshToken").value(LOGIN_PASSWORD));
    }
}