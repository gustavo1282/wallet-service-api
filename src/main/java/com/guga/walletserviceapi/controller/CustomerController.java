package com.guga.walletserviceapi.controller;

import com.guga.walletserviceapi.model.Customer;
import com.guga.walletserviceapi.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("${controller.path.base}/customers")
@Tag(name = "Customer", description = "Endpoints for managing customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "Create a new Customer", description = "Creates a new Customer with the data provided in the request body.")
    @PostMapping("/customer")
    public ResponseEntity<Customer> createCustomer(@RequestBody Customer customer) {
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
    public ResponseEntity<Customer> getCustomerById(@PathVariable Long id) {
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
    public ResponseEntity<List<Customer>> getAllCustomers(Pageable pageable) {

        Page<Customer> pageCustomers = customerService.getAllCustomers(pageable);

        if (pageCustomers == null || pageCustomers.isEmpty() || pageCustomers.stream().toList().isEmpty()) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<List<Customer>>(pageCustomers.stream().toList(), HttpStatus.OK);

    }

}
