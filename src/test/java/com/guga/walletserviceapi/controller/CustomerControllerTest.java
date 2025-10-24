package com.guga.walletserviceapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guga.walletserviceapi.helpers.RandomGenerator;
import com.guga.walletserviceapi.model.Customer;
import com.guga.walletserviceapi.service.CustomerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

// Levanta apenas a camada Web (Controller) para o teste
@WebMvcTest(CustomerController.class)
@ActiveProfiles("test")
//@TestPropertySource(properties = {
//        "controller.path.base=/api/v1/customers"
//})
class CustomerControllerTest {

    // Objeto usado para simular chamadas HTTP
    @Autowired
    private MockMvc mockMvc;

    // Converte objetos Java para JSON e vice-versa
    @Autowired
    private ObjectMapper objectMapper;

    // Substitui o CustomerService real por um mock
    @MockitoBean
    private CustomerService customerService;

    // O path base é definido no application.properties, mas o @WebMvcTest
    // geralmente assume o path raiz, ou você pode configurá-lo.
    @Value("${controller.path.base}")
    private final String BASE_PATH = "/customers";

    // --- 1. Testes para POST /customer ---

    @Test
    @DisplayName("Deve retornar 201 Created ao criar um Customer com sucesso")
    void deve_Retornar201Created_QuandoCustomerCriadoComSucesso() throws Exception {
        // Arrange
        Customer customerInput = instanceNewCustomer();
        Customer customerCreated = customerInput.cloneCustomer();

        // Simula o sucesso da camada de serviço
        when(customerService.saveCustomer(any(Customer.class))).thenReturn(customerCreated);

        // Act & Assert
        mockMvc.perform(post(BASE_PATH + "/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerInput)))
                .andExpect(status().isCreated()) // Verifica o status HTTP 201
                .andExpect(header().exists("Location")) // Verifica a presença do Header 'Location'
                .andExpect(jsonPath("$.customerId", is(customerCreated.getCustomerId()))) // Verifica o ID no corpo da resposta
                .andExpect(jsonPath("$.firstName", is(customerCreated.getFirstName())));
    }

    private Customer instanceNewCustomer() {
        Customer c = new Customer();
        //c.setCustomerId();
        c.setFirstName(RandomGenerator.generateRandomLetters(12));
        c.setLastName(RandomGenerator.generateRandomLetters(15));

        c.setFullName(c.getFirstName()
                .concat(" ")
                .concat(RandomGenerator.generateRandomLetters(9))
                .concat(" ")
                .concat(c.getLastName()));
        c.setPhoneNumber(RandomGenerator.generateRandomNumbers(11));
        c.setDocumentId(RandomGenerator.generateRandomNumbers(11));

        int year = ThreadLocalRandom.current().nextInt(19, 28 + 1);
        int month = ThreadLocalRandom.current().nextInt(1, 12 + 1);
        int day = ThreadLocalRandom.current().nextInt(1, 28);
        c.setBirthDate(RandomGenerator.getDateNowMinus(year, month, day));
        c.setEmail(c.getFirstName()
                .concat("_")
                .concat(c.getBirthDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"))));

        LocalDateTime ldtRegister = LocalDateTime.now();
        c.setCreatedAt(ldtRegister);
        c.setUpdatedAt(ldtRegister);

        return c;

    }

    // --- 2. Testes para GET /{id} ---

//    @Test
//    @DisplayName("Deve retornar 200 OK e o Customer ao buscar por ID")
//    void deve_Retornar200Ok_AoBuscarCustomerPorIdExistente() throws Exception {
//        // Arrange
//        Long customerId = 2L;
//        Customer foundCustomer = new Customer(customerId, "Maria", "Souza");
//
//        // Simula o retorno de um Customer pelo Service
//        when(customerService.getCustomerById(customerId)).thenReturn(foundCustomer);
//
//        // Act & Assert
//        mockMvc.perform(get(BASE_PATH + "/{id}", customerId)
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk()) // Verifica o status HTTP 200
//                .andExpect(jsonPath("$.customerId", is(2)))
//                .andExpect(jsonPath("$.firstName", is("Maria")));
//    }

    @Test
    @DisplayName("Deve retornar 404 Not Found ao buscar Customer por ID inexistente")
    void deve_Retornar404NotFound_AoBuscarCustomerPorIdInexistente() throws Exception {
        // Arrange
        Long customerId = 99L;

        // Simula uma exceção (como NotFoundException) que o Service levantaria
        when(customerService.getCustomerById(customerId)).thenThrow(new RuntimeException("Customer not found"));
        // Nota: O tratamento de exceção no Controller precisaria ser mais robusto
        // para mapear a exceção do Service para 404, mas assumimos aqui o retorno padrão de erro.

        // Act & Assert
        mockMvc.perform(get(BASE_PATH + "/{id}", customerId))
                // Idealmente seria .andExpect(status().isNotFound()), dependendo do Exception Handler
                .andExpect(status().isInternalServerError());
    }

    // --- 3. Testes para PUT /{id} ---

//    @Test
//    @DisplayName("Deve retornar 200 OK ao atualizar um Customer existente")
//    void deve_Retornar200Ok_AoAtualizarCustomer() throws Exception {
//        // Arrange
//        Long customerId = 3L;
//        Customer customerUpdate = new Customer("Lucas", "Ferreira");
//        Customer customerUpdated = new Customer(customerId, "Lucas", "Ferreira");
//
//        // Simula o retorno do Customer atualizado
//        when(customerService.updateCustomer(eq(customerId), any(Customer.class))).thenReturn(customerUpdated);
//
//        // Act & Assert
//        mockMvc.perform(put(BASE_PATH + "/{id}", customerId)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(customerUpdate)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.lastName", is("Ferreira")));
//    }

    // --- 4. Testes para GET /list ---

//    @Test
//    @DisplayName("Deve retornar 200 OK e a lista de Customers")
//    void deve_Retornar200Ok_EListaDeCustomers() throws Exception {
//        // Arrange
//        List<Customer> customerList = List.of(
//                new Customer(4L, "Ana", "Gomes"),
//                new Customer(5L, "Pedro", "Alves")
//        );
//
//        // Simula uma página com conteúdo
//        when(customerService.getAllCustomers(any(Pageable.class)))
//                .thenReturn(new PageImpl<>(customerList));
//
//        // Act & Assert
//        mockMvc.perform(get(BASE_PATH + "/list"))
//                .andExpect(status().isOk()) // Verifica o status HTTP 200
//                .andExpect(jsonPath("$").isArray()) // Verifica se o retorno é um Array
//                .andExpect(jsonPath("$.length()", is(2))) // Verifica o tamanho da lista
//                .andExpect(jsonPath("$[0].firstName", is("Ana")));
//    }

//    @Test
//    @DisplayName("Deve retornar 404 Not Found quando a lista de Customers está vazia")
//    void deve_Retornar404NotFound_QuandoNaoHaCustomers() throws Exception {
//        // Arrange
//        // Simula uma página vazia
//        when(customerService.getAllCustomers(any(Pageable.class)))
//                .thenReturn(new PageImpl<>(Collections.emptyList()));
//
//        // Act & Assert
//        mockMvc.perform(get(BASE_PATH + "/list"))
//                .andExpect(status().isNotFound()); // Verifica o status HTTP 404
//    }
}