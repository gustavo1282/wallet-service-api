package com.guga.walletserviceapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.fasterxml.jackson.core.type.TypeReference;
import com.guga.walletserviceapi.dto.customer.CustomerMapperImpl;
import com.guga.walletserviceapi.exception.ResourceBadRequestException;
import com.guga.walletserviceapi.exception.ResourceNotFoundException;
import com.guga.walletserviceapi.helpers.FileUtils;
import com.guga.walletserviceapi.helpers.RandomMock;
import com.guga.walletserviceapi.model.Customer;
import com.guga.walletserviceapi.model.LoginAuth;
import com.guga.walletserviceapi.model.ParamApp;
import com.guga.walletserviceapi.model.Routers;
import com.guga.walletserviceapi.model.Wallet;
import com.guga.walletserviceapi.model.enums.LoginRole;
import com.guga.walletserviceapi.service.CustomerService;

@WebMvcTest(controllers = CustomerController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({CustomerMapperImpl.class})
class CustomerControllerTests extends BaseControllerTest {

    @MockitoBean
    private CustomerService customerService;

    // =========================================================
    // CONTEXTO PÚBLICO / CRIAÇÃO
    // =========================================================

    @Nested
    @DisplayName("Operações de Criação (Geralmente Públicas ou Admin)")
    class CreationContext {

        @Test
        @DisplayName("Deve criar um novo Customer com sucesso")
        void createCustomer_created() throws Exception {
            // Arrange
            setupMockAuth(List.of(LoginRole.ADMIN));
            Customer newCustomer = transactionUtilsMock.createCustomerMock(nextCustomerId());
            when(customerService.saveCustomer(any(Customer.class))).thenReturn(newCustomer);
            customers.add(newCustomer);

            // Act & Assert
            performRequest(HttpMethod.POST, Routers.CUSTOMERS, newCustomer, null)
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
            // Arrange
            LoginAuth loginAuthMock = setupMockAuth(List.of(LoginRole.USER));
            Customer mockCustomer = getCustomerById(loginAuthMock.getCustomerId());
            when(customerService.getCustomerById(mockCustomer.getCustomerId())).thenReturn(mockCustomer);

            // Act & Assert
            performRequest(HttpMethod.GET, Routers.CUSTOMERS + "/me", null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(mockCustomer.getCustomerId()))
                .andExpect(jsonPath("$.fullName").value(mockCustomer.getFullName()));
        }

        @Test
        @DisplayName("Deve atualizar os dados do próprio customer")
        void updateMyCustomer_ok() throws Exception {
            // Arrange
            LoginAuth auth = setupMockAuth(List.of(LoginRole.USER));
            Customer mockCustomer = getCustomerById(auth.getCustomerId());
            Customer updatedCustomer = mockCustomer.toBuilder()
                    .status(transactionUtilsMock.defineStatus())
                    .phoneNumber(RandomMock.generatePhoneNumberAleatory())
                    .updatedAt(LocalDateTime.now())
                    .build();
            when(customerService.updateCustomer(eq(mockCustomer.getCustomerId()), any(Customer.class)))
                    .thenReturn(updatedCustomer);

            // Act & Assert
            performRequest(HttpMethod.PUT, Routers.CUSTOMERS + "/me", updatedCustomer, null)
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
            // Arrange
            LoginAuth auth = setupMockAuth(List.of(LoginRole.ADMIN));
            Customer mockCustomer = getCustomerById(auth.getCustomerId());
            when(customerService.getCustomerById(mockCustomer.getCustomerId())).thenReturn(mockCustomer);

            // Act & Assert
            String url = Routers.CUSTOMERS + "/" + mockCustomer.getCustomerId();
            performRequest(HttpMethod.GET, url, null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(mockCustomer.getCustomerId()));
        }

        @Test
        @DisplayName("Admin deve receber 404 ao buscar ID inexistente")
        void getCustomerById_notFound() throws Exception {
            // Arrange
            setupMockAuth(List.of(LoginRole.ADMIN));
            Long nonExistentId = nextCustomerId() + 283L;
            when(customerService.getCustomerById(nonExistentId))
                .thenThrow(new ResourceNotFoundException("Customer not found"));

            // Act & Assert
            String url = Routers.CUSTOMERS + "/" + nonExistentId;
            performRequest(HttpMethod.GET, url, null, null)
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Deve retornar erro/vazio quando filtro não retorna resultados")
        void listCustomers_emptyOrError() throws Exception {
            // Arrange
            setupMockAuth(List.of(LoginRole.ADMIN));
            when(customerService.filterByStatus(isNull(), any(Pageable.class)))
                .thenThrow(new ResourceNotFoundException("Customers not found"));

            // Act & Assert
            performRequest(HttpMethod.GET, Routers.CUSTOMERS, null, null)
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Admin deve atualizar Customer por ID")
        void updateCustomerById_ok() throws Exception {
            // Arrange
            LoginAuth auth = setupMockAuth(List.of(LoginRole.ADMIN));
            Customer mockCustomer = getCustomerById(auth.getCustomerId());
            Customer updatedCustomer = mockCustomer.toBuilder()
                .status(transactionUtilsMock.defineStatus())
                .phoneNumber(RandomMock.generatePhoneNumberAleatory())
                .updatedAt(LocalDateTime.now())
                .build();
            when(customerService.updateCustomer(eq(mockCustomer.getCustomerId()), any(Customer.class)))
                    .thenReturn(updatedCustomer);

            // Act & Assert
            String url = Routers.CUSTOMERS + "/" + mockCustomer.getCustomerId();
            performRequest(HttpMethod.PUT, url, mockCustomer, null)
                .andExpect(status().isOk());
        }
    }

    // =========================================================
    // AUXILIARES
    // =========================================================

    private long nextCustomerId() {
        return customers.stream()
            .mapToLong(Customer::getCustomerId)
            .max()
            .orElse(0L) + 1;
    }

    private Customer getCustomerById(Long customerId) {
        return customers.stream()
            .filter(c -> c.getCustomerId().equals(customerId))
            .findFirst()
            .orElseThrow(() -> new ResourceBadRequestException(String.format("Customer %d não encontrado para o LoginAuth", customerId)));
    }

    @Override
    public void loadMockData() {
        paramsApp = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_PARAMS_APP, new TypeReference<List<ParamApp>>() {});
        customers = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_CUSTOMER, new TypeReference<List<Customer>>() {});      
        wallets = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_WALLET, new TypeReference<List<Wallet>>() {});
        loginAuths = dataPersistenceService.importJson(FileUtils.SEED_FOLDER_DEFAULT + FileUtils.JSON_FILE_LOGIN_AUTH, new TypeReference<List<LoginAuth>>() {});
    }

}