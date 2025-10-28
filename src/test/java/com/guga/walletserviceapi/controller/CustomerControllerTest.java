package com.guga.walletserviceapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guga.walletserviceapi.config.ResourceNotFoundException;
import com.guga.walletserviceapi.helpers.RandomGenerator;
import com.guga.walletserviceapi.model.Customer;
import com.guga.walletserviceapi.model.enums.Status;
import com.guga.walletserviceapi.service.CustomerService;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(CustomerController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(username = "user", roles = {"USER"})
@RequiredArgsConstructor
class CustomerControllerTest {

    // Objeto usado para simular chamadas HTTP
    @Autowired
    private MockMvc mockMvc;

    // Converte objetos Java para JSON e vice-versa
    @Autowired
    private ObjectMapper objectMapper;

    // Substitui o CustomerService real por um mock
    @MockitoBean
    private final CustomerService customerService;

    @Value("${controller.path.base}")
    private String BASE_PATH;

    private String API_NAME = "/customers";

    private String URI_API;

    private Faker faker;

    List<Customer> customerList;

    @BeforeEach
    void setup() {
        faker = new Faker(new Locale("pt-BR"));
        createCustomerListMock();
        URI_API = BASE_PATH.concat(API_NAME);
        System.out.println(("BeforeEach - OK"));
    }

    @Test
    @DisplayName("Deve retornar 201 Created ao criar um Customer com sucesso")
     void shouldReturn201_WhenCreateNewCustomer() throws Exception {
        // Arrange
        Customer customerInput = customerList.get(RandomGenerator.generateIntNumberByInterval(0, customerList.size()));
        Customer customerCreated = customerInput.cloneCustomer();

        // Simula o sucesso da camada de serviço
        when(customerService.saveCustomer(any(Customer.class))).thenReturn(customerCreated);

        // Act & Assert
        mockMvc.perform(post(URI_API.concat("/customer"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerInput)))
                .andExpect(status().isCreated()) // Verifica o status HTTP 201
                .andExpect(header().exists("Location")) // Verifica a presença do Header 'Location'
                .andExpect(jsonPath("$.customerId", is(customerCreated.getCustomerId().intValue()))) // Verifica o ID no corpo da resposta
                //.andExpect(jsonPath("$.firstName", is(customerCreated.getFirstName())))
        //is(32L), Long.class)
            ;
    }

    // --------------------------------------------------------------------------------
    // 2. Teste para GET /{id} (getCustomerById)
    // --------------------------------------------------------------------------------
    @Test
    @DisplayName("Deve retornar 200 OK e o Customer ao buscar por ID")
    void shouldReturn200_WhenRequestCustomer_WhenExists() throws Exception {

        int indexCustomer = RandomGenerator.generateIntNumberByInterval(0, 50);
        Customer foundCustomer = customerList.get(indexCustomer).cloneCustomer();
        Long customerId = foundCustomer.getCustomerId();

        // Simula o retorno de um Customer pelo Service
        when(customerService.getCustomerById(customerId)).thenReturn(foundCustomer);

        // Act & Assert
        mockMvc.perform(get(URI_API.concat("/{id}"), customerId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // Verifica o status HTTP 200
                .andExpect(jsonPath("$.customerId", is(customerId.intValue())))
            ;
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found ao buscar Customer por ID inexistente")
    void shouldReturn404NotFound_WhenSearchingForCustomer_WithNonExistentId() throws Exception {

        Long customerId = (long)RandomGenerator
                .generateIntNumberByInterval(customerList.size()+2 , customerList.size() + 100);

        when(customerService.getCustomerById(customerId)).thenThrow(new ResourceNotFoundException("Customer not found"));

        mockMvc.perform(get(URI_API.concat("/{id}"), customerId))
                .andExpect(status().isNotFound());
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

    @Test
    @DisplayName("Deve retornar 200 OK e a lista de Customers")
    void shouldReturn200_WhenRequestingCustomerList_WithValidParameters() throws Exception {

        Status statusFilter = Status
                .fromValue(RandomGenerator.generateIntNumberByInterval(0, Status.values().length));

        List<Customer> customersFiltered = customerList.stream()
                .filter(customer -> customer.getStatus().equals(statusFilter) &&
                        customer.getCustomerId() > 0)
                .collect(Collectors.toList());

        // Simula uma página com conteúdo
        when(customerService.getAllCustomers(any(Pageable.class)))
                .thenReturn(new PageImpl<>(customersFiltered));

        // Act & Assert
        mockMvc.perform(get(URI_API.concat("/list")))
                .andExpect(status().isOk()) // Verifica o status HTTP 200
                .andExpect(jsonPath("$").isArray()) // Verifica se o retorno é um Array
                .andExpect(jsonPath("$.length()", is(customersFiltered.size()))) // Verifica o tamanho da lista
                .andExpect(jsonPath("$[0].firstName", is(customersFiltered.get(0).getFirstName())));
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found quando a lista de Customers está vazia")
    void shouldReturn404WhenNoCustomersFound() throws Exception {
        // Arrange
        // Simula uma página vazia
        when(customerService.getAllCustomers(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        // Act & Assert
        mockMvc.perform(get(URI_API.concat("/list")))
                .andExpect(status().isNotFound()); // Verifica o status HTTP 404
    }

    private void createCustomerListMock() {
        customerList = new ArrayList<Customer>();

        customerList = IntStream.range(0, 50)
                .mapToObj(i -> {
                    Customer c = new Customer();
                    c.setCustomerId( (long)i+1 );

                    c.setStatus(Status.fromValue(RandomGenerator.generateIntNumberByInterval(1, Status.values().length-1)));

                    c.setFullName(RandomGenerator.removeSufixoEPrevixos( faker.name().fullName().toUpperCase() ));
                    String[] partName = c.getFullName().split(" ");
                    c.setFirstName(partName[0]);
                    c.setLastName(partName[partName.length-1]);

                    int year = RandomGenerator.generateIntNumberByInterval(19, 29);
                    int month = ThreadLocalRandom.current().nextInt(1, 12 + 1);
                    int day = ThreadLocalRandom.current().nextInt(1, 28);
                    c.setBirthDate(RandomGenerator.getDateNowMinus(year, month, day));

                    c.setEmail(faker.internet().emailAddress(c.getFirstName()
                            .concat(".")
                            .concat(c.getLastName())
                            .concat(".")
                            .concat(String.valueOf(c.getBirthDate().getMonthValue()))
                            .concat(String.valueOf(c.getBirthDate().getYear()))
                    ));

                    c.setPhoneNumber(faker.phoneNumber().cellPhone());
                    c.setDocumentId(faker.idNumber().valid());
                    c.setCpf(faker.cpf().valid());

                    c.setCreatedAt(RandomGenerator.generatePastLocalDateTime(2));
                    c.setUpdatedAt(c.getCreatedAt());


                    System.out.println(c.toString());

                    return c;
                })
                .toList();
    }


}