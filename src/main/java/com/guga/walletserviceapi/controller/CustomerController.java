package com.guga.walletserviceapi.controller;

import java.net.URI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
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

import com.guga.walletserviceapi.logging.LogMarkers;
import com.guga.walletserviceapi.model.AuditContext;
import com.guga.walletserviceapi.model.Customer;
import com.guga.walletserviceapi.model.enums.Status;
import com.guga.walletserviceapi.service.CustomerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("${controller.path.base}/customers")
@Tag(name = "Customer", description = "Endpoints for managing customers")
@RequiredArgsConstructor
public class CustomerController {

    private static final Logger logger =
            LogManager.getLogger(CustomerController.class);
            
    private final CustomerService customerService;

    @Value("${spring.data.web.pageable.default-page-size}")
    private int defaultPageSize;    


    @Operation(summary = "Create a new Customer", description = "Creates a new Customer with the data provided in the request body.")
    @PostMapping("/customer")
    public ResponseEntity<Customer> createCustomer(
        @RequestBody Customer customer,
        HttpServletRequest httpRequest
        ) 
    {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        AuditContext auditContext = AuditContext.builder()
            .sessionId(httpRequest.getSession().getId())
            .userAgent(httpRequest.getHeader("User-Agent"))
            .ipAddress(httpRequest.getRemoteAddr())
            .username(authentication.getName())
            .build();

        // 🧭 LOG + TRACE
        logger.info(LogMarkers.LOG,
                "Request received to create customer | user={}", auditContext.getUsername());

        // 🛡️ AUDIT (entrada)
        logger.info(LogMarkers.AUDIT,
                "CREATE_CUSTOMER_START | auditContext={}", auditContext);

        Customer createdCustomer = customerService.saveCustomer(customer);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdCustomer.getCustomerId())
                .toUri();

        // 🛡️ AUDIT (resultado)
        logger.info(LogMarkers.AUDIT,
                "CREATE_CUSTOMER_SUCCESS | customerId={}", createdCustomer.getCustomerId());

        return ResponseEntity.created(location)
            .header("X-Trace-Id", ThreadContext.get("traceId"))
            .body(createdCustomer);
    }


    @Operation(summary = "Update customer by ID", description = "Updates a Customer by their ID provided in the request body.")
    @PutMapping("/{id}")
    public ResponseEntity<Customer> updateCustomer(
            @PathVariable Long id,
            @RequestBody @Valid Customer customerUpdate
            ) 
    {
        Customer customer = customerService.updateCustomer(id, customerUpdate);
        return new ResponseEntity<>(customer, HttpStatus.OK);
    }


    @Operation(summary = "Get customer by ID", description = "Retrieves a Customer by their ID provided in the request body.")
    @GetMapping("/{id}")
    public ResponseEntity<Customer> getById(
        @PathVariable Long id
        ) 
    {
        Customer customer = customerService.getCustomerById(id);
        return new ResponseEntity<>(customer, HttpStatus.OK);
    }


    @Operation(summary = "Get all customers", description = "Retrieves all customers.")
    @GetMapping("/list")
    public ResponseEntity<Page<Customer>> list(
            @RequestParam(name = "status", required = false) Status status,        
            @RequestParam(defaultValue = "0") int page
        ) 
    {

        Pageable pageable = PageRequest.of(page, defaultPageSize,
                Sort.by(
                    Sort.Order.asc("createdAt"),
                    Sort.Order.asc("status")
                )
            );

        Page<Customer> resultCustomers = customerService.filterByStatus(status, pageable);

        return new ResponseEntity<>(resultCustomers, HttpStatus.OK);
    }


}
