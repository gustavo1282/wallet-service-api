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
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.fasterxml.jackson.core.type.TypeReference;
import com.guga.walletserviceapi.exception.ResourceBadRequestException;
import com.guga.walletserviceapi.helpers.FileUtils;
import com.guga.walletserviceapi.helpers.RandomMock;
import com.guga.walletserviceapi.helpers.TransactionUtilsMock;
import com.guga.walletserviceapi.model.Customer;
import com.guga.walletserviceapi.model.LoginAuth;
import com.guga.walletserviceapi.model.ParamApp;
import com.guga.walletserviceapi.model.Wallet;
import com.guga.walletserviceapi.model.enums.LoginRole;
import com.guga.walletserviceapi.service.WalletService;

@WebMvcTest(WalletController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        //com.guga.walletserviceapi.config.ConfigProperties.class,
        com.guga.walletserviceapi.config.TestPasswordConfig.class,
        com.guga.walletserviceapi.service.common.DataPersistenceService.class
})
class WalletControllerTests extends BaseControllerTest {

    private static final String API_NAME = "/wallets";

    @MockitoBean
    private WalletService walletService;


    @BeforeAll
    void setupOnce() {
        this.URI_API = getBaseUri(API_NAME);
        loadMockData();
    }

    @BeforeEach
    void setUp() {
        Mockito.reset(
            this.jwtAuthenticationFilter,            
            this.jwtService,
            this.authenticationManager
        );
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
            LoginAuth auth = setupMockAuth(List.of(LoginRole.USER));

            Wallet mockWallet = getWalletById(auth.getWalletId());

            when(walletService.getWalletById(auth.getWalletId())).thenReturn(mockWallet);

            mockMvc.perform(get(URI_API + "/me"))
                .andDo(result -> System.out.println("Status: " + result.getResponse().getStatus()))
                .andDo(result -> System.out.println("Body: " + result.getResponse().getContentAsString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(mockWallet.getWalletId()));
        }

        @Test
        @DisplayName("Deve listar todas as wallets do customer autenticado")
        void getMyWallets_ok() throws Exception {
            LoginAuth auth = setupMockAuth(List.of(LoginRole.USER));

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

            LoginAuth auth = setupMockAuth(List.of(LoginRole.ADMIN));

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

            LoginAuth auth = setupMockAuth(List.of(LoginRole.ADMIN));

            Wallet mockWallet = getAleatoryWallet();

            when(walletService.getWalletById(mockWallet.getWalletId())).thenReturn(mockWallet);

            mockMvc.perform(get(URI_API + "/{id}", mockWallet.getWalletId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.walletId").value(mockWallet.getWalletId()));
        }

        @Test
        @DisplayName("Admin deve atualizar uma wallet")
        void updateWallet_ok() throws Exception {

            LoginAuth auth = setupMockAuth(List.of(LoginRole.ADMIN));

            Wallet mockWallet = getAleatoryWallet();
            mockWallet.setStatus(TransactionUtilsMock.defineStatus());

            when(walletService.updateWallet(eq(mockWallet.getWalletId()), any(Wallet.class)))
                    .thenReturn(mockWallet);

            wallets.add(mockWallet);

            mockMvc.perform(put(URI_API + "/{id}", mockWallet.getWalletId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mockWallet)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Admin deve listar todas as wallets de forma paginada")
        void listWallets_ok() throws Exception {
            LoginAuth auth = setupMockAuth(List.of(LoginRole.ADMIN));

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
            LoginAuth auth = setupMockAuth(List.of(LoginRole.ADMIN));

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

    @Override
    public void loadMockData() {
        paramsApp = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_PARAMS_APP, new TypeReference<List<ParamApp>>() {});
        customers = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_CUSTOMER, new TypeReference<List<Customer>>() {});      
        wallets = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_WALLET, new TypeReference<List<Wallet>>() {});
        loginAuths = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_LOGIN_AUTH, new TypeReference<List<LoginAuth>>() {});
    }

    // Helper auxiliar para buscar o login pelo ID da carteira na massa de dados
    private LoginAuth findLoginAuthByWalletId(long walletId) {
        return loginAuths.stream()
                .filter(la -> la.getWalletId().equals(walletId))
                .findFirst()
                .orElse(null);
    }
 
    private Wallet getWalletById(Long walletId) {
        return wallets.stream()
            .filter(w -> w.getWalletId().equals(walletId))
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