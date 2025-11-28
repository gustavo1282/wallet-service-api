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

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
import com.guga.walletserviceapi.model.enums.Status;
import com.guga.walletserviceapi.service.CustomerService;

import net.datafaker.Faker;


//@SpringBootTest
@WebMvcTest(CustomerController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@WithMockUser(username = "user", roles = {"USER"})
class CustomerControllerTests {

    // Objeto usado para simular chamadas HTTP
    @Autowired
    private MockMvc mockMvc;

    // Converte objetos Java para JSON e vice-versa
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomerService customerService;

    private static final String API_NAME = "/customers";

    private String URI_API;

    private Faker faker;

    private List<Customer> customers;

    @Autowired
    private Environment env;

    @BeforeEach
    void setup() {
        Mockito.reset(customerService);
        faker = new Faker(new Locale("pt-BR"));
        customers = TransactionUtilsMock.createCustomerListMock();

        

        URI_API = env.getProperty("server.protocol-type")
                    .concat("://" + env.getProperty("server.hostname"))
                    .concat(":" + env.getProperty("server.port") + "/")
                    .concat(env.getProperty("server.servlet.context-path"))
                    .concat(env.getProperty("controller.path.base"))
                    .concat(API_NAME)
                    ;

        System.out.println(("BeforeEach - OK"));
    }


    @Test
    @DisplayName("Deve retornar 201 Created ao criar um Customer com sucesso")
    void shouldReturn201_WhenCreateNewCustomer() throws Exception {
        // Arrange
        Customer customerInput = customers.get(RandomMock.generateIntNumberByInterval(0, customers.size()-1));
        Customer customerCreated = customerInput.toBuilder().build();

        // Simula o sucesso da camada de serviço
        when(customerService.saveCustomer(any(Customer.class))).thenReturn(customerCreated);

        // Act & Assert
        mockMvc.perform(post(URI_API.concat("/customer"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(customerInput)))

                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.customerId", is(customerCreated.getCustomerId().intValue())))
                .andExpect(jsonPath("$.cpf", is(customerCreated.getCpf())))
        ;
    }

    // --------------------------------------------------------------------------------
    // Teste para GET /{id} (getCustomerById)
    // --------------------------------------------------------------------------------
    @Test
    @DisplayName("Deve retornar 200 OK e o Customer ao buscar por ID")
    void shouldReturn200_WhenRequestCustomer_WhenExists() throws Exception {

        int indexCustomer = RandomMock.generateIntNumberByInterval(0, 50);
        Customer foundCustomer = customers.get(indexCustomer).toBuilder().build();
        Long customerId = foundCustomer.getCustomerId();

        // Simula o retorno de um Customer pelo Service
        when(customerService.getCustomerById(customerId)).thenReturn(foundCustomer);

        // Act & Assert
        mockMvc.perform(get(URI_API.concat("/{id}"), customerId)
                .contentType(MediaType.APPLICATION_JSON))

                .andExpect(status().isOk()) // Verifica o status HTTP 200
                .andExpect(jsonPath("$.customerId", is(customerId.intValue())))
                .andExpect(jsonPath("$.cpf", is(foundCustomer.getCpf())))
            ;
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found ao buscar Customer por ID inexistente")
    void shouldReturn404NotFound_WhenSearchingForCustomer_WithNonExistentId() throws Exception {

        Long customerId = (long) RandomMock
                .generateIntNumberByInterval(customers.size()+2 , customers.size() + 100);

        when(customerService.getCustomerById(customerId)).thenThrow(new ResourceNotFoundException("Customer not found"));

        mockMvc.perform(get(URI_API.concat("/{id}"), customerId))
                .andExpect(status().isNotFound());
    }


    @Test
    @DisplayName("Deve retornar 200 OK ao atualizar um Customer existente")
    void sholdReturn200_WhenUpdateCustomerExistentByID() throws Exception {

        Customer customerInput = getMockCustomer();

        Long customerId = customerInput.getCustomerId();

        when(customerService.getCustomerById(customerId)).thenReturn(customerInput);

        Customer customerUpdated = customerInput.toBuilder()
                .status(TransactionUtilsMock.defineStatus())
                .phoneNumber(faker.phoneNumber().cellPhone())
                .build();

        // Simula o retorno do Customer atualizado
        when(customerService.updateCustomer(eq(customerId), any(Customer.class)))
                .thenReturn(customerUpdated);

        mockMvc.perform(put(URI_API + "/{id}", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerInput))
                )
                .andReturn();

    }

    private Customer getMockCustomer() {
        int indexCustomer = RandomMock.generateIntNumberByInterval(0, customers.size() - 1);
        return customers.get(indexCustomer);
    }

    // --- 4. Testes para GET /list ---

    @Test
    @DisplayName("Deve retornar 200 OK e a lista de Customers")
    void shouldReturn200_WhenRequestingCustomerList_WithValidParameters() throws Exception {

        Status statusFilter = TransactionUtilsMock.defineStatus();

        List<Customer> customersFiltered = customers.stream()
                .filter(customer -> customer.getStatus().equals(statusFilter) &&
                        customer.getCustomerId() > 0)
                .toList();

        // Simula uma página com conteúdo
        when(customerService.filterByStatus(eq(statusFilter), any(Pageable.class)))
                .thenReturn(new PageImpl<>(customersFiltered));

        // Act & Assert
        mockMvc.perform(get(URI_API.concat("/list"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()", is(customersFiltered.size())))
                .andExpect(jsonPath("$[0].firstName", is(customersFiltered.get(0).getFirstName())));
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found quando a lista de Customers está vazia")
    void shouldReturn404WhenNoCustomersFound() throws Exception {
        // Arrange
        // Simula uma página vazia
        when(customerService.filterByStatus( any(Status.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        // Act & Assert
        mockMvc.perform(get(URI_API.concat("/list")))
                .andExpect(status().isNotFound()); // Verifica o status HTTP 404
    }

//    private void createCustomerListMock() {
//        customerList = new ArrayList<>();
//
//        IntStream.range(0, 100)
//                .forEach(i -> {
//
//                    String fullName = RandomGenerator.removeSufixoEPrevixos( faker.name().fullName().toUpperCase() );
//                    String[] partName =  fullName.split(" ");
//                    LocalDate birthDate = HelperTest.defineBirthDateMore18YearOld();
//                    LocalDateTime dtCreatedAt = RandomGenerator.generatePastLocalDateTime(2);
//
//                    Customer CustomerNew = Customer.builder()
//                            .customerId((long) (i + 1))
//                            .status(HelperTest.defineStatus())
//                            .fullName(fullName)
//                            .firstName(partName[0])
//                            .lastName( partName[partName.length-1] )
//                            .birthDate(birthDate)
//                            .email(faker.internet().emailAddress( partName[0].concat(".")
//                                    .concat( partName[partName.length -1 ] ).concat(".")
//                                    .concat( String.valueOf(birthDate.getMonthValue()) )
//                                    .concat( String.valueOf(birthDate.getYear()) )
//                            ))
//                            .phoneNumber(faker.phoneNumber().cellPhone())
//                            .documentId(faker.idNumber().valid())
//                            .cpf(faker.cpf().valid())
//                            .createdAt(dtCreatedAt)
//                            .updatedAt(dtCreatedAt)
//
//                            .build();
//
//                    customerList.add(CustomerNew);
//
//                    }
//                );
//    }

}