package com.guga.walletserviceapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.guga.walletserviceapi.helpers.GlobalHelper;
import com.guga.walletserviceapi.model.LoginAuth;
import com.guga.walletserviceapi.model.enums.LoginAuthType;
import com.guga.walletserviceapi.repository.LoginAuthRepository;
import com.guga.walletserviceapi.security.JwtService;
import com.guga.walletserviceapi.service.LoginAuthService;


@ActiveProfiles("test")
@WithMockUser(username = "user", roles = {"USER"})
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTests {
// --- DEPENDÊNCIAS (Mocks) ---
    @Autowired
    private MockMvc mockMvc;
    
    @MockitoBean
    private AuthenticationManager authenticationManager;
    
    @MockitoBean
    private JwtService jwtService;
    
    @MockitoBean
    private LoginAuthService loginAuthService;
    
    @MockitoBean
    private LoginAuthRepository loginAuthRepository;
    
    @MockitoBean 
    private PasswordEncoder passwordEncoder; 

    // --- VARIÁVEIS DE TESTE ---
    @Value("${controller.path.base}")
    private String BASE_PATH;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private static final String API_NAME = "/auth";
    private static final String LOGIN_USER = "l_guga";
    private static final String LOGIN_PASSWORD = "p_ruts"; // Senha em texto plano

    private static final String TAG_MOCK_ACCESS_TOKEN = "mock-access-token";
    private static final String TAG_MOCK_REFRESH_TOKEN = "mock-refresh-token"; 
    private static final String NEW_PASSWORD_REGISTER = "NOVA-SENHA-REGISTER"; 
    
    private final PasswordEncoder realEncoder = new BCryptPasswordEncoder();
    private String URI_API;
    private Map<String, String> tokens;
    private String jsonLoginRequest;
    private String passwordEncodedRegister;

    @BeforeEach
    void setup() {
        URI_API = BASE_PATH + API_NAME;

        tokens = GlobalHelper.jwtTokens(LOGIN_USER);
        
        jsonLoginRequest = String.format("""
            {
              "username": "%s",
              "password": "%s"
            }
            """, LOGIN_USER, LOGIN_PASSWORD);

        when(passwordEncoder.encode(anyString()))
            .thenAnswer(new Answer<String>() {
                @Override
                public String answer(InvocationOnMock invocation) throws Throwable {
                    String rawPassword = invocation.getArgument(0);
                    return realEncoder.encode(rawPassword);
                }
            });

        passwordEncodedRegister = realEncoder.encode(LOGIN_PASSWORD);

    }


    @Test
    void testLoginReturnsTokens() throws Exception {

        Authentication successfulAuthentication = new UsernamePasswordAuthenticationToken(
            LOGIN_USER, 
            null, 
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(successfulAuthentication);

        when(jwtService.generateAccessToken(eq(LOGIN_USER)))
            .thenReturn(tokens.get(TAG_MOCK_ACCESS_TOKEN));
            
        when(jwtService.generateRefreshToken(eq(LOGIN_USER)))
            .thenReturn(tokens.get(TAG_MOCK_REFRESH_TOKEN));

        mockMvc.perform(post(URI_API + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonLoginRequest))
                .andDo(result -> System.out.println("Status: " + result.getResponse().getStatus()))
                .andDo(result -> System.out.println("Body: " + result.getResponse().getContentAsString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(tokens.get(TAG_MOCK_ACCESS_TOKEN)))
                .andExpect(jsonPath("$.refreshToken").value(tokens.get(TAG_MOCK_REFRESH_TOKEN)));
    }

    @Test
    void testRegisterReturnsEncodedPassword() throws Exception {

        LoginAuth mockLoginAuth = createLoginAuthMock();
        
        String newPasswordRegistrer = realEncoder.encode(NEW_PASSWORD_REGISTER);
        mockLoginAuth.setAccessKey(newPasswordRegistrer);

        //when(loginAuthRepository.findByLogin(LOGIN_USER))
        //    .thenReturn(java.util.Optional.of(mockLoginAuth));

        when(loginAuthService.register(anyString(), anyString()))
            .thenReturn(mockLoginAuth);

        mockMvc.perform(post(URI_API + "/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonLoginRequest))
            .andDo(result -> System.out.println("Status: " + result.getResponse().getStatus()))
            .andDo(result -> System.out.println("Body: " + result.getResponse().getContentAsString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.login").value(LOGIN_USER))
            .andExpect(jsonPath("$.accessKey").value(newPasswordRegistrer))
            ;
    }

    @Test
    void testRefreshGeneratesNewAccessToken() throws Exception {
        
        Map<String, String> newTokenGenerate = GlobalHelper.jwtTokens(LOGIN_USER);

        when(jwtService.validateToken(anyString())).thenReturn(true);
        when(jwtService.extractUsername(anyString())).thenReturn(LOGIN_USER);

        when(jwtService.generateAccessToken(anyString())).thenReturn(newTokenGenerate.get(TAG_MOCK_ACCESS_TOKEN));
        when(jwtService.generateRefreshToken(anyString())).thenReturn(newTokenGenerate.get(TAG_MOCK_REFRESH_TOKEN));

        mockMvc.perform(post(URI_API + "/refresh")
            .param("refreshToken", tokens.get(TAG_MOCK_ACCESS_TOKEN)))
            .andDo(result -> System.out.println("Status: " + result.getResponse().getStatus()))
            .andDo(result -> System.out.println("Body: " + result.getResponse().getContentAsString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.refreshToken").value(tokens.get(TAG_MOCK_ACCESS_TOKEN)))
            .andExpect(jsonPath("$.accessToken").value(newTokenGenerate.get(TAG_MOCK_ACCESS_TOKEN)))
            ;
    }

    private LoginAuth createLoginAuthMock() {
        LocalDateTime now = LocalDateTime.now();
        return LoginAuth.builder()
            .id( 1L )
            .customerId(1L)
            .login(LOGIN_USER)
            .accessKey(passwordEncodedRegister)
            .loginAuthType(LoginAuthType.USER_NAME)
            .createdAt(now)
            .updatedAt(now)
            .build();
    }

}