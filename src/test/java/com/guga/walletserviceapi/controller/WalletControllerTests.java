package com.guga.walletserviceapi.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guga.walletserviceapi.exception.ResourceNotFoundException;
import com.guga.walletserviceapi.helpers.RandomMock;
import com.guga.walletserviceapi.helpers.TransactionUtilsMock;
import com.guga.walletserviceapi.model.Customer;
import com.guga.walletserviceapi.model.Wallet;
import com.guga.walletserviceapi.security.JwtAuthenticationFilter;
import com.guga.walletserviceapi.security.JwtService;
import com.guga.walletserviceapi.service.LoginAuthService;
import com.guga.walletserviceapi.service.WalletService;

import net.datafaker.Faker;


@WebMvcTest(WalletController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(username = "user", roles = {"USER"})
class WalletControllerTests {

    @Autowired
    private MockMvc mockMvc;

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private JwtAuthenticationFilter jwtAuthenticationFilter;

	@MockitoBean
	private LoginAuthService loginAuthService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WalletService walletService;

    @Value("${controller.path.base}")
    private String BASE_PATH;

    private static final String API_NAME = "/wallets";

    private String URI_API;

    private Faker faker;

    private List<Customer> customers;

    private List<Wallet> wallets;

    @BeforeEach
    void before() {
        Mockito.reset(walletService);
        faker = new Faker(new Locale("pt-BR"));
        customers = generateCustomersMock();
        wallets = TransactionUtilsMock.createWalletListMock( customers );
        URI_API = BASE_PATH + API_NAME;
    }

    @Cacheable(value = "customers", key = "#customersMock")
        private List<Customer> generateCustomersMock() {
                return (List<Customer>)TransactionUtilsMock.createCustomerListMock();
        }



    @Test
    @DisplayName("Deve retornar 201 - Criar novo Wallet")
    void shouldReturn201_WhenCreateNewWallet() throws Exception {

        int indexWallet = (int) RandomMock.generateIntNumberByInterval(0, wallets.size() - 1);
        Wallet wallet = wallets.get(indexWallet);
        Long walletId = wallet.getWalletId();
        wallet.setWalletId(null);

        Wallet walletCreated = wallet.toBuilder().build();

        when(walletService.saveWallet(any(Wallet.class))).thenReturn(walletCreated);
        walletCreated.setWalletId(walletId);
        Long customerId = walletCreated.getCustomer().getCustomerId();

        // Act & Assert
        mockMvc.perform(post(URI_API.concat("/wallet"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(walletCreated)))

                //.andDo(print())
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))

                .andExpect(jsonPath("$.walletId", is(walletId.intValue())));

    }

    @Test
    @DisplayName("Deve retornar 200 - Listar todas as wallets por paginação")
    public void shouldReturn200_WhenRequestAllWallets_ReturnByPagable() throws Exception {

        Pageable pageable = PageRequest.of(0, 50, Sort.by("walletId").ascending() );

        List<Wallet> mockWallets = new ArrayList<>( wallets.stream().toList() );
        Page<Wallet> mockPage = new PageImpl<>(mockWallets, pageable, mockWallets.size());

        when(walletService.getAllWallets(any(Pageable.class))).thenReturn(mockPage);

        // Act & Assert
        mockMvc.perform(get(URI_API.concat("/list"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists())                
                .andExpect(jsonPath("$.page.totalElements").value(mockPage.getContent().size()));
        ;

    }

    @Test
    @DisplayName("Deve retornar 200 - Quando filtrar por Wallet Id")
    public void shouldReturn200_WhenRequestWalletById() throws Exception {

        int indexWallet = (int) RandomMock.generateIntNumberByInterval(0, wallets.size() - 1);
        Wallet walletMock = wallets.get(indexWallet);
        Long walletIdMock = walletMock.getWalletId();

        when(walletService.getWalletById(walletIdMock)).thenReturn(walletMock);

        // Act & Assert
        mockMvc.perform(get(URI_API.concat("/{id}"), walletIdMock)
                .contentType(MediaType.APPLICATION_JSON))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId", is( walletIdMock.intValue() )));

    }

    @Test
    @DisplayName("Deve retornar 400 - Quando tenta localizar uma Wallet por ID inválido")
    public void shouldReturn400_WhenRequestWalletByIdNotExists() throws Exception {

        Long walletIdMock = wallets.get(wallets.size()-1).getWalletId() + 12;

        when(walletService.getWalletById(walletIdMock))
                .thenThrow(new ResourceNotFoundException("Wallet not found. ID: " + walletIdMock));


        mockMvc.perform(get(URI_API.concat("/{id}"), walletIdMock))
                .andExpect(status().isNotFound());

    }


    @Test
    @DisplayName("Deve retornar 200-OK ao atualizar uma Wallet existente")
    void sholdReturn200_WhenUpdateWalletExistentByID() throws Exception {

        int indexWallet = (int) RandomMock.generateIntNumberByInterval(0, wallets.size() - 1);
        Wallet walletMock = wallets.get(indexWallet);
        Long walletIdMock = walletMock.getWalletId();

        Wallet walletUpdated = walletMock.toBuilder()
                .status(TransactionUtilsMock.defineStatus())
                .updatedAt(LocalDateTime.now())
                .build();

        // Simula o retorno do Customer atualizado
        when(walletService.updateWallet(eq(walletIdMock), any(Wallet.class)))
                .thenReturn(walletUpdated);

        mockMvc.perform(put(URI_API + "/{id}", walletIdMock)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(walletUpdated))
                )
                .andReturn();
    }


//    @Test
//    @DisplayName("Deve retornar 200-OK - Quando desejar obter a(s) wallet(s) por um Customer ID específico")
//    void sholdReturn200_WhenRequestListWalletsByCustomerId() throws Exception {
//
//        List<Customer> uniqueCustomers = wallets.stream()
//                .map(Wallet::getCustomer)
//                .distinct()
//                .collect(Collectors.toList());
//
//        int indexCustomer = (int) RandomMock.generateIntNumberByInterval(0, uniqueCustomers.size() - 1);
//        Customer customerRequest = uniqueCustomers.get(indexCustomer);
//        Long customerIdInput = customerRequest.getCustomerId();
//
//        List<Wallet> walletsUniqueCustomer = wallets.stream()
//                .filter(c -> c.getCustomerId().equals(customerIdInput))
//                .toList()
//                ;
//
//        when(walletService.getWalletByCustomerId(customerIdInput))
//                .thenReturn(walletsUniqueCustomer);
//
//        // Act & Assert
//        mockMvc.perform(get(URI_API.concat("/customer/list"), customerIdInput)
//                        .contentType(MediaType.APPLICATION_JSON))
//
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.customerId", is( customerIdInput.intValue() )));
//
//
//
//    }


}
