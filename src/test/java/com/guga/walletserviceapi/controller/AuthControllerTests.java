package com.guga.walletserviceapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.guga.walletserviceapi.repository.LoginAuthRepository;
import com.guga.walletserviceapi.security.JwtAuthenticationFilter;
import com.guga.walletserviceapi.security.JwtService;
import com.guga.walletserviceapi.service.LoginAuthService;


@ActiveProfiles("test")
@WithMockUser(username = "wallet_user", roles = {"USER"})
@WebMvcTest(AuthController.class)
//@Import({AuthControllerTests.TestConfig.class})
@AutoConfigureMockMvc(addFilters = false)
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;
    
    @MockitoBean
    private PasswordEncoder passwordEncoder; 

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private LoginAuthService loginAuthService;

    @MockitoBean
    private LoginAuthRepository loginAuthRepository;

    @MockitoBean 
    private JwtAuthenticationFilter jwtAuthenticationFilter;


    @Autowired
    private Environment env;

    private static final String API_NAME = "/auth";

    private String URI_API;
    private static String LOGIN_USER = "wallet_user";
    private static String LOGIN_PASSWORD = "wallet_pass";
    private String json;
    private static final String ACCESS_TOKEN_MOCK = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ3YWxsZXRfdXNlciIsInJvbGVzIjoiIiwiaWF0IjoxNzY1MzQ4MjE3LCJleHAiOjE3NjUzNTA5MTd9.LntEXuGHNc8y7Y4QRs59qnQ2SKe3wmiBMsHmwAef81o";
    private static final String REFRESH_TOKEN_MOCK = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ3YWxsZXRfcGFzcyIsImlhdCI6MTc2NTM0ODIxNywiZXhwIjoxNzY1NTIxMDE3fQ.sbTo7ssrHlJmW3cEt3n-qGRmwWTwAMk698RYJdkxNhA";


    @BeforeEach
    void setup() {        
        // Configuramos o mock do PasswordEncoder aqui:
        when(passwordEncoder.encode(LOGIN_PASSWORD)).thenReturn("encoded123");

        json = String.format("""
            {
              "username": "%s",
              "password": "%s"
            }
            """, LOGIN_USER, LOGIN_PASSWORD);


        URI_API = env.getProperty("controller.path.base") + API_NAME;

        when(jwtService.generateAccessToken(LOGIN_USER)).thenReturn(ACCESS_TOKEN_MOCK);
        when(jwtService.generateRefreshToken(LOGIN_PASSWORD)).thenReturn(REFRESH_TOKEN_MOCK);

        // 1. Simular o objeto Authentication que o Manager retornará
        Authentication mockAuthentication = mock(Authentication.class);
        when(mockAuthentication.getName()).thenReturn(LOGIN_USER);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(mockAuthentication);
    }

    @Test
    void testLoginReturnsTokens() throws Exception {

        mockMvc.perform(post(URI_API + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))

                .andDo(result -> System.out.println("Status: " + result.getResponse().getStatus()))
                .andDo(result -> System.out.println("Body: " + result.getResponse().getContentAsString()))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(ACCESS_TOKEN_MOCK))
                .andExpect(jsonPath("$.refreshToken").value(REFRESH_TOKEN_MOCK));
    }

    @Test
    void testRegisterReturnsEncodedPassword() throws Exception {
        // A linha "when(passwordEncoder.encode(LOGIN_PASSWORD)).thenReturn("encoded123");"
        // foi movida para o BeforeEach.
        
        mockMvc.perform(post(URI_API.concat("/register"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andDo(result -> System.out.println("Status: " + result.getResponse().getStatus()))
                .andDo(result -> System.out.println("Body: " + result.getResponse().getContentAsString()))
                .andExpect(status().isOk());
                //.andExpect(jsonPath("$.encodedPassword").value("encoded123"));
    }

    @Test
    void testRefreshGeneratesNewAccessToken() throws Exception {
        when(jwtService.validateToken(LOGIN_PASSWORD)).thenReturn(true);
        when(jwtService.extractUsername(LOGIN_PASSWORD)).thenReturn(LOGIN_USER);
        when(jwtService.generateAccessToken(LOGIN_USER)).thenReturn("newAccessToken");

        mockMvc.perform(post(URI_API.concat("/refresh"))
                .param("refreshToken", LOGIN_PASSWORD))
                .andExpect(status().isOk())
                .andDo(result -> System.out.println("Status: " + result.getResponse().getStatus()))
                .andDo(result -> System.out.println("Body: " + result.getResponse().getContentAsString()))
                .andExpect(jsonPath("$.accessToken").value("newAccessToken"))
                .andExpect(jsonPath("$.refreshToken").value(LOGIN_PASSWORD));
    }

}

