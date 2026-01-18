package com.guga.walletserviceapi.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guga.walletserviceapi.exception.ResourceBadRequestException;
import com.guga.walletserviceapi.exception.ResourceNotFoundException;
import com.guga.walletserviceapi.helpers.FileUtils;
import com.guga.walletserviceapi.helpers.RandomMock;
import com.guga.walletserviceapi.helpers.TransactionUtilsMock;
import com.guga.walletserviceapi.model.Customer;
import com.guga.walletserviceapi.model.LoginAuth;
import com.guga.walletserviceapi.model.ParamApp;
import com.guga.walletserviceapi.model.Wallet;
import com.guga.walletserviceapi.model.enums.LoginRole;
import com.guga.walletserviceapi.model.enums.Status;
import com.guga.walletserviceapi.security.JwtAuthenticationDetails;
import com.guga.walletserviceapi.security.auth.JwtAuthenticatedUserProvider;
import com.guga.walletserviceapi.security.filter.JwtAuthenticationFilter;
import com.guga.walletserviceapi.service.CustomerService;
import com.guga.walletserviceapi.service.common.DataPersistenceService;

@WebMvcTest(CustomerController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Import({
	//com.guga.walletserviceapi.config.PasswordConfig.class,
	com.guga.walletserviceapi.service.common.DataPersistenceService.class
})
class CustomerControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomerService customerService;

    // Essencial para evitar o erro de ApplicationContext load failure
    @MockitoBean
    private JwtAuthenticatedUserProvider jwtAuthenticatedUserProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

	@Autowired
    private DataPersistenceService dataPersistenceService;

	//@Autowired
    //private PasswordEncoder passwordEncoder;

    @Value("${controller.path.base}")
    private String BASE_PATH;

    private static final String API_NAME = "/customers";    

    private String URI_API;
	private List<ParamApp> paramsApp;
    private List<Customer> customers;
	private List<Wallet> wallets;
	private List<LoginAuth> loginAuths;

    private boolean dataLoaded;

    @BeforeEach
    void setUp() {
        URI_API = BASE_PATH + API_NAME;
        loadMockData();
    }    

    // =========================================================
    // CONTEXTO PÚBLICO / CRIAÇÃO
    // =========================================================

    @Nested
    @DisplayName("Operações de Criação (Geralmente Públicas ou Admin)")
    class CreationContext {

        @Test
        @DisplayName("Deve criar um novo Customer com sucesso")
        void createCustomer_created() throws Exception {

			LoginAuth auth = setupMockAuth(List.of("USER"));

            Customer newCustomer = TransactionUtilsMock.createCustomerMock(nextCustomerId());
            
            // Simula o salvamento
            when(customerService.saveCustomer(any(Customer.class))).thenReturn(newCustomer);

            mockMvc.perform(post(URI_API) // Assumindo POST na raiz /customers
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newCustomer)))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.customerId").value(newCustomer.getCustomerId()))
                    .andExpect(jsonPath("$.cpf").value(newCustomer.getCpf()));
        }
    }

    // =========================================================
    // CONTEXTO DE USUÁRIO (Endereços /me)
    // =========================================================

    @Nested
    @DisplayName("Operações do Usuário Autenticado")
    class UserContext {

        @Test
        @DisplayName("Deve retornar os dados do próprio customer autenticado")
        void getMyCustomer_ok() throws Exception {

			LoginAuth auth = setupMockAuth(List.of("USER"));

            Customer mockCustomer = getCustomerById(auth.getCustomerId());

            when(customerService.getCustomerById(mockCustomer.getCustomerId())).thenReturn(mockCustomer);

            mockMvc.perform(get(URI_API + "/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.customerId").value(mockCustomer.getCustomerId()))
                    .andExpect(jsonPath("$.fullName").value(mockCustomer.getFullName()));
        }

        @Test
        @DisplayName("Deve atualizar os dados do próprio customer")
        void updateMyCustomer_ok() throws Exception {

			LoginAuth auth = setupMockAuth(List.of("USER"));

            Customer mockCustomer = getCustomerById(auth.getCustomerId());

            // Prepara objeto atualizado
            Customer updatedCustomer = mockCustomer.toBuilder()
					.status(TransactionUtilsMock.defineStatus())
                    .phoneNumber(RandomMock.generatePhoneNumberAleatory())
					.updatedAt(LocalDateTime.now())
                    .build();

            when(customerService.updateCustomer(eq(mockCustomer.getCustomerId()), any(Customer.class)))
                    .thenReturn(updatedCustomer);

            mockMvc.perform(put(URI_API + "/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updatedCustomer)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.phoneNumber").value(updatedCustomer.getPhoneNumber()));
        }
    }

    // =========================================================
    // CONTEXTO ADMINISTRATIVO
    // =========================================================

    @Nested
    @DisplayName("Operações Administrativas")
    class AdminContext {

        @Test
        @DisplayName("Admin deve obter Customer por ID")
        void getCustomerById_ok() throws Exception {
			LoginAuth auth = setupMockAuth(List.of("ADMIN"));

            Customer mockCustomer = getAleatoryCustomer();

            when(customerService.getCustomerById(mockCustomer.getCustomerId())).thenReturn(mockCustomer);

            mockMvc.perform(get(URI_API + "/{id}", mockCustomer.getCustomerId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.customerId").value(mockCustomer.getCustomerId()));
        }

        @Test
        @DisplayName("Admin deve receber 404 ao buscar ID inexistente")
        void getCustomerById_notFound() throws Exception {
			LoginAuth auth = setupMockAuth(List.of("ADMIN"));

            Long nonExistentId = nextCustomerId() + 283L;

            when(customerService.getCustomerById(nonExistentId))
                .thenThrow(new ResourceNotFoundException("Customer not found"));

            mockMvc.perform(get(URI_API + "/{id}", nonExistentId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Admin deve listar Customers filtrando por Status")
        void listCustomers_ok() throws Exception {
            LoginAuth auth = setupMockAuth(List.of("ADMIN"));
            
            Status statusFilter = TransactionUtilsMock.defineStatus();
            List<Customer> customersFiltered = customers.stream()
                    .filter(c -> c.getStatus().equals(statusFilter))
                    .limit(10)
                    .toList();

            when(customerService.filterByStatus(eq(statusFilter), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(customersFiltered));

            mockMvc.perform(get(URI_API) // Assumindo GET na raiz /customers
                    .param("status", statusFilter.name())
                    .param("page", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(customersFiltered.size())));
        }
        
        @Test
        @DisplayName("Deve retornar erro/vazio quando filtro não retorna resultados")
        void listCustomers_emptyOrError() throws Exception {
            LoginAuth auth = setupMockAuth(List.of("ADMIN"));

            when(customerService.filterByStatus(isNull(), any(Pageable.class)))
                .thenThrow(new ResourceNotFoundException("Customers not found"));

            mockMvc.perform(get(URI_API))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Admin deve atualizar Customer por ID")
        void updateCustomerById_ok() throws Exception {

            LoginAuth auth = setupMockAuth(List.of("ADMIN"));

            Customer mockCustomer = getAleatoryCustomer();

			Customer updatedCustomer = mockCustomer.toBuilder()
				.status(TransactionUtilsMock.defineStatus())
				.phoneNumber(RandomMock.generatePhoneNumberAleatory())
				.updatedAt(LocalDateTime.now())
				.build();
			

            when(customerService.updateCustomer(eq(mockCustomer.getCustomerId()), any(Customer.class)))
                    .thenReturn(mockCustomer);

            mockMvc.perform(put(URI_API + "/{id}", mockCustomer.getCustomerId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mockCustomer)))
                    .andExpect(status().isOk());
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

        // Garante que o provider injetado no Controller retorne o nosso mock
        when(jwtAuthenticatedUserProvider.get()).thenReturn(details);
		return login;
    }

    private Customer getAleatoryCustomer() {
        int idx = RandomMock.generateIntNumberByInterval(0, customers.size() - 1);
        return customers.get(idx);
    }

	private long nextCustomerId() {
		return customers.stream()
			.mapToLong(Customer::getCustomerId)
			.max()
			.orElse(0L) + 1;
	}


    private void loadMockData() {
        if (!dataLoaded) {
            paramsApp = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_PARAMS_APP, new TypeReference<List<ParamApp>>() {});
            customers = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_CUSTOMER, new TypeReference<List<Customer>>() {});
            wallets = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_WALLET, new TypeReference<List<Wallet>>() {});
            loginAuths = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_LOGIN_AUTH, new TypeReference<List<LoginAuth>>() {});
        }
    }

    private LoginAuth getRandomLoginByRole(List<String> rolesStr) {
        // 1. Converter as roles alvo para Enum
        List<LoginRole> targetEnums = rolesStr.stream()
                .map(role -> LoginRole.valueOf(role.trim().toUpperCase()))
                .toList();

		Set<Long> activeCustomerIds = customers.stream()
				.filter(c -> Status.ACTIVE.equals(c.getStatus()))
				.map(Customer::getCustomerId)
				.collect(Collectors.toSet());

        // 3. Filtrar Logins
    	return loginAuths.stream()
            // Filtro de Role
            .filter(la -> la.getRole().stream().anyMatch(targetEnums::contains))
            
            // Filtro de Status (Lookup imediato no Set)
            // Se o ID do login estiver no conjunto de ativos, passa.
            .filter(la -> activeCustomerIds.contains(la.getCustomerId()))
            
            .findAny()
            .orElseThrow(() -> new RuntimeException(
                "Nenhum LoginAuth vinculado a um Customer ATIVO foi encontrado para as roles: " + rolesStr));
    }

	

    // private void encriptAccessKeyLoginAuthListMock(List<LoginAuth> loginAuths) {
    //     for (LoginAuth loginAuth : loginAuths) {
    //             String rawAccessKey = loginAuth.getAccessKey();
    //             String encodedAccessKey = passwordEncoder.encode(rawAccessKey);
    //             loginAuth.setAccessKey(encodedAccessKey);
    //     }
    // }

    private Customer getCustomerById(Long customerId) {
        return customers.stream()
            .filter(c -> c.getCustomerId().equals(customerId))
            .findFirst()
            .orElseThrow(() -> new ResourceBadRequestException(String.format("Customer %d não encontrado para o LoginAuth", customerId)));
    }

}