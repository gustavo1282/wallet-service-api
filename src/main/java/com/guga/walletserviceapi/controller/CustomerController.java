package com.guga.walletserviceapi.controller;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.guga.walletserviceapi.model.Customer;
import com.guga.walletserviceapi.model.enums.Status;
import com.guga.walletserviceapi.service.CustomerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("${controller.path.base}/customers")
@Tag(name = "Customer", description = "Endpoints for managing wallets")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    public static final String CUSTOMER_REQUEST_EXAMPLE = """
      {
        "status": "ACTIVE",
        "cpf": "482.625.121-11",
        "documentId": "258.868.057-70",
        "phoneNumber": "(38) 7982-1593",
        "birthDate": "1999-12-26",
        "firstName": "YAGO",
        "lastName": "SOARES",
        "fullName": "YAGO SOARES",
        "email": "yago.soares.121999@gmail.com",
        "createdAt": "2024-11-04 13:19:20.000000000",
        "updatedAt": "2024-11-04 13:19:20.000000000"
      }
      """;

    @Value("${spring.data.web.pageable.default-page-size}")
    private int defaultPageSize;

    // -------------------------------------
    // --- Criar um pedido
    // -------------------------------------

    @Operation(summary = "Create a new Customer", description = "Creates a new Customer with the data provided in the request body.")
    @PostMapping("/customer")
    public ResponseEntity<Customer> createCustomer(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Dados do novo cliente a ser criado.",
            required = true,
            content = @Content(
                mediaType = "application/json",
                // Define o exemplo que aparecerá na documentação
                examples = @ExampleObject(
                    name = "Exemplo Completo",
                    value = CUSTOMER_REQUEST_EXAMPLE,
                    summary = "Exemplo de criação de um cliente."
                ),
                // Referencia a estrutura de dados que este endpoint espera
                schema = @Schema(implementation = Customer.class)
            )
        )
        @RequestBody
        Customer customer) {


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();


        Customer createdCustomer = customerService.saveCustomer(customer);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdCustomer.getCustomerId())
                .toUri();

        return ResponseEntity.created(location).body(createdCustomer);

    }


    @Operation(summary = "Get customer by ID", description = "Retrieves a Customer by their ID provided in the request body.")
    @GetMapping("/{id}")
    public ResponseEntity<Customer> getById(@PathVariable Long id) {

        Customer customer = customerService.getCustomerById(id);

        return new ResponseEntity<>(customer, HttpStatus.OK);
        
    }

    @Operation(summary = "Update customer by ID", description = "Updates a Customer by their ID provided in the request body.")
    @PutMapping("/{id}")
    public ResponseEntity<Customer> updateCustomer(
            @PathVariable Long id,
            @RequestBody @Valid Customer customerUpdate) {
        Customer customer = customerService.updateCustomer(id, customerUpdate);
        return new ResponseEntity<>(customer, HttpStatus.OK);
    }


    @Operation(summary = "Get all customers", description = "Retrieves all customers.")
    @GetMapping("/list")
    public ResponseEntity<Page<Customer>> list(
        @RequestParam(name = "status", required = false) 
            Status status,
        @RequestParam(defaultValue = "0") int page) {

        Sort sort = Sort.by("status").ascending()
                .and(Sort.by("updatedAt").descending());

        Pageable pageable = PageRequest.of(page, defaultPageSize, sort);

        Page<Customer> customerResult = customerService.filterByStatus(status, pageable);

        return new ResponseEntity<>(customerResult, HttpStatus.OK);
    }


}
