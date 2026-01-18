package com.guga.walletserviceapi.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guga.walletserviceapi.exception.ResourceBadRequestException;
import com.guga.walletserviceapi.helpers.FileUtils;
import com.guga.walletserviceapi.helpers.RandomMock;
import com.guga.walletserviceapi.helpers.TransactionUtilsMock;
import com.guga.walletserviceapi.model.Customer;
import com.guga.walletserviceapi.model.LoginAuth;
import com.guga.walletserviceapi.model.ParamApp;
import com.guga.walletserviceapi.model.Wallet;
import com.guga.walletserviceapi.model.enums.LoginRole;
import com.guga.walletserviceapi.security.JwtAuthenticationDetails;
import com.guga.walletserviceapi.security.auth.JwtAuthenticatedUserProvider;
import com.guga.walletserviceapi.security.filter.JwtAuthenticationFilter;
import com.guga.walletserviceapi.service.WalletService;
import com.guga.walletserviceapi.service.common.DataPersistenceService;

@WebMvcTest(WalletController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Import({
        //com.guga.walletserviceapi.config.ConfigProperties.class,
        com.guga.walletserviceapi.config.PasswordConfig.class,
        com.guga.walletserviceapi.service.common.DataPersistenceService.class
})
class WalletControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WalletService walletService;

    @MockitoBean
    private JwtAuthenticatedUserProvider jwtAuthenticatedUserProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DataPersistenceService dataPersistenceService;

    @Value("${controller.path.base}")
    private String BASE_PATH;

    private static final String API_NAME = "/wallets";

    private String URI_API;
    private List<ParamApp> paramsApp;
    private List<Customer> customers;
    private List<Wallet> wallets;
    private List<LoginAuth> loginAuths;

    private boolean dataLoaded;

    @BeforeAll
    void setUpAll() {
        URI_API = BASE_PATH + API_NAME;
        loadMockData();
    }        

    // =========================================================
    // CONTEXTO DE USUÁRIO (Endereços /me)
    // =========================================================

    @Nested
    @DisplayName("Operações do Usuário Autenticado")
    class UserContext {

        @Test
        @DisplayName("Deve retornar a wallet do usuário autenticado")
        void getMyWallet_ok() throws Exception {
            LoginAuth auth = setupMockAuth(List.of("USER"));

            Wallet mockWallet = getWalletById(auth.getWalletId());

            when(walletService.getWalletById(auth.getWalletId())).thenReturn(mockWallet);

            mockMvc.perform(get(URI_API + "/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.walletId").value(mockWallet.getWalletId()));
        }

        @Test
        @DisplayName("Deve listar todas as wallets do customer autenticado")
        void getMyWallets_ok() throws Exception {
            LoginAuth auth = setupMockAuth(List.of("USER"));

            List<Wallet> mockWallets = wallets.stream()
                .filter(w -> w.getCustomerId().equals(auth.getCustomerId()))
                .collect(java.util.stream.Collectors.toList());
            
            when(walletService.getWalletByCustomerId(eq(auth.getCustomerId()), any()))
                .thenReturn(new PageImpl<>(mockWallets));

            mockMvc.perform(get(URI_API + "/me/all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(mockWallets.size())));
        }
    }

    // =========================================================
    // CONTEXTO ADMINISTRATIVO
    // =========================================================

    @Nested
    @DisplayName("Operações Administrativas")
    class AdminContext {

        @Test
        @DisplayName("Admin deve criar uma nova wallet com sucesso")
        void createWallet_created() throws Exception {           

            LoginAuth auth = setupMockAuth(List.of("ADMIN"));

            Wallet mockNewWallet = Wallet.builder()
                .walletId(nextWalletId())
                .customerId(auth.getCustomerId())
                .status(TransactionUtilsMock.defineStatus())
                .build();

            Wallet walletResult = TransactionUtilsMock.generateNewWallet(mockNewWallet);

            when(walletService.saveWallet(any(Wallet.class))).thenReturn(walletResult);

            mockMvc.perform(post(URI_API)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(walletResult)))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.walletId").value(walletResult.getWalletId()));
        }

        @Test
        @DisplayName("Admin deve obter wallet por ID")
        void getWalletById_ok() throws Exception {

            LoginAuth auth = setupMockAuth(List.of("ADMIN"));

            Wallet mockWallet = getAleatoryWallet();

            when(walletService.getWalletById(mockWallet.getWalletId())).thenReturn(mockWallet);

            mockMvc.perform(get(URI_API + "/{id}", mockWallet.getWalletId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.walletId").value(mockWallet.getWalletId()));
        }

        @Test
        @DisplayName("Admin deve atualizar uma wallet")
        void updateWallet_ok() throws Exception {

            LoginAuth auth = setupMockAuth(List.of("ADMIN"));

            Wallet mockWallet = getAleatoryWallet();
            mockWallet.setStatus(TransactionUtilsMock.defineStatus());

            when(walletService.updateWallet(eq(mockWallet.getWalletId()), any(Wallet.class)))
                    .thenReturn(mockWallet);

            mockMvc.perform(put(URI_API + "/{id}", mockWallet.getWalletId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mockWallet)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Admin deve listar todas as wallets de forma paginada")
        void listWallets_ok() throws Exception {
            LoginAuth auth = setupMockAuth(List.of("ADMIN"));

            when(walletService.getAllWallets(any(), any()))
                .thenReturn(new PageImpl<>(wallets));

            mockMvc.perform(get(URI_API)
                    .param("page", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").exists());
        }

        @Test
        @DisplayName("Admin deve listar wallets por ID do Customer")
        void getWalletsByCustomer_ok() throws Exception {
            LoginAuth auth = setupMockAuth(List.of("ADMIN"));

            // PASSO 1: Criar um mapa de "ID do Cliente" -> "Quantidade de Wallets"
            Map<Long, Long> contagemPorCliente = wallets.stream()
                .collect(Collectors.groupingBy(
                    w -> w.getCustomer().getCustomerId(), // Agrupa pelo ID
                    Collectors.counting()                 // Conta quantos existem
                ));

            long customerId = contagemPorCliente.entrySet().stream()
                .filter(entry -> entry.getValue() > 0) // Filtra apenas clientes com pelo menos 1 wallet
                .findAny()
                .map(Map.Entry::getKey)
                .orElseThrow(() -> new ResourceBadRequestException("Nenhum cliente encontrado com wallets na massa de dados."));

            List<Wallet> filteredWallets = wallets.stream()
                .filter(w -> w.getCustomerId().equals(customerId))
                .collect(Collectors.toList());

            when(walletService.getWalletByCustomerId(eq(customerId), any()))
                .thenReturn(new PageImpl<>(filteredWallets));

            mockMvc.perform(get(URI_API + "/by-customer/{customerId}", customerId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").exists());
        }
    }

    // =========================================================
    // AUXILIARES
    // =========================================================

    private LoginAuth setupMockAuth(List<String> roles) {
        LoginAuth login = getRandomLoginByRole(roles);

        JwtAuthenticationDetails details = JwtAuthenticationDetails.builder()
                .loginId(login.getId())
                .login(login.getLogin())
                .walletId(login.getWalletId())
                .customerId(login.getCustomerId())
                .roles(login.getRole())
                .build();

        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .toList();

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(details, null, authorities)
        );

        when(jwtAuthenticatedUserProvider.get()).thenReturn(details);
        return login;
    }

    private void loadMockData() {
        if (!dataLoaded) {
            paramsApp = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_PARAMS_APP, new TypeReference<List<ParamApp>>() {});
            customers = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_CUSTOMER, new TypeReference<List<Customer>>() {});      
            wallets = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_WALLET, new TypeReference<List<Wallet>>() {});
            loginAuths = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_LOGIN_AUTH, new TypeReference<List<LoginAuth>>() {});
        }
    }

    private void encriptAccessKeyLoginAuthListMock(List<LoginAuth> loginAuths) {
        for (LoginAuth loginAuth : loginAuths) {
                String rawAccessKey = loginAuth.getAccessKey();
                String encodedAccessKey = passwordEncoder.encode(rawAccessKey);
                loginAuth.setAccessKey(encodedAccessKey);
        }
    }


    private LoginAuth getRandomLoginByRole(List<String> rolesStr) {
        // 1. Converter as roles alvo para Enum
        List<LoginRole> targetEnums = rolesStr.stream()
                .map(role -> LoginRole.valueOf(role.trim().toUpperCase()))
                .toList();

        // 4. Buscar o primeiro LoginAuth que possua a Role desejada e pertença a uma dessas transações
        return wallets.stream()
                .map(t -> findLoginAuthByWalletId(t.getWalletId()))
                .filter(Objects::nonNull)
                .filter(la -> la.getRole().stream()
                        .anyMatch(r -> targetEnums.contains(r)))
                .findFirst()
                .orElseThrow(() -> new ResourceBadRequestException(
                    "Não foi possível encontrar um LoginAuth com as roles " + rolesStr + " associado a uma transação de sucesso."));    
    }

    // Helper auxiliar para buscar o login pelo ID da carteira na massa de dados
    private LoginAuth findLoginAuthByWalletId(long walletId) {
        return loginAuths.stream()
                .filter(la -> la.getWalletId() == walletId)
                .findFirst()
                .orElse(null);
    }
 
    private Wallet getWalletById(Long walletId) {
        return wallets.stream()
            .filter(w -> w.getWalletId() == walletId)
            .findFirst()
            .orElseThrow(() -> new ResourceBadRequestException("Wallet não encontrada para o LoginAuth"));
    }

    private Wallet getAleatoryWallet() {
        int idxWallet = RandomMock.generateIntNumberByInterval(0, wallets.size() - 1);
        return wallets.get(idxWallet);
    }

    private long nextWalletId() {
        return wallets.stream()
            .mapToLong(Wallet::getWalletId)
            .max()
            .orElse(0L) + 1;
    }

}