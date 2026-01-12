package com.guga.walletserviceapi.controller;

import java.net.URI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.guga.walletserviceapi.audit.AuditLogContext;
import com.guga.walletserviceapi.logging.LogMarkers;
import com.guga.walletserviceapi.model.Customer;
import com.guga.walletserviceapi.model.enums.Status;
import com.guga.walletserviceapi.security.JwtAuthenticationDetails;
import com.guga.walletserviceapi.security.auth.JwtAuthenticatedUserProvider;
import com.guga.walletserviceapi.service.CustomerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("${controller.path.base}/customers")
@Tag(name = "Customer", description = "Customer domain operations")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class CustomerController {

    private static final Logger LOGGER = LogManager.getLogger(CustomerController.class);

    private final CustomerService customerService;
    private final JwtAuthenticatedUserProvider authUserProvider;

    @Value("${spring.data.web.pageable.default-page-size}")
    private int defaultPageSize;

    // =====================================================
    // USER CONTEXT
    // =====================================================

    @Operation(
        summary = "Get authenticated customer profile",
        description = "Returns customer data associated with the authenticated user (JWT context)."
    )
    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Customer> getMyCustomer() {

        JwtAuthenticationDetails authDetails = authUserProvider.get();
        Long customerId = authDetails.getCustomerId();

        LOGGER.info(LogMarkers.LOG,
            "GET_CUSTOMER_ME | customerId={} login={}",
            customerId, authDetails.getLogin()
        );

        return ResponseEntity.ok(
            customerService.getCustomerById(customerId)
        );
    }

    // =====================================================
    // ADMIN CONTEXT
    // =====================================================

    @Operation(
        summary = "Create a new customer",
        description = "Creates a new customer. Admin-only operation."
    )
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Customer> createCustomer(
        @RequestBody @Valid Customer customer
    ) {

        AuditLogContext auditContext = AuditLogContext.from(authUserProvider.get());

        LOGGER.info(LogMarkers.LOG,
            "CREATE_CUSTOMER | admin={}",
            auditContext.getUsername()
        );

        Customer createdCustomer = customerService.saveCustomer(customer);

        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{customerId}")
            .buildAndExpand(createdCustomer.getCustomerId())
            .toUri();

        return ResponseEntity
            .created(location)
            .header("X-Trace-Id", auditContext.getTraceId())
            .body(createdCustomer);
    }

    @Operation(
        summary = "Update customer by ID",
        description = "Updates an existing customer by ID. Admin-only operation."
    )
    @PutMapping("/{customerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Customer> updateCustomer(
        @PathVariable Long customerId,
        @RequestBody @Valid Customer customerUpdate
    ) {

        LOGGER.info(LogMarkers.LOG,
            "UPDATE_CUSTOMER | admin={} customerId={}",
            authUserProvider.getLogin(), customerId
        );

        return ResponseEntity.ok(
            customerService.updateCustomer(customerId, customerUpdate)
        );
    }

    @Operation(
        summary = "Get customer by ID",
        description = "Retrieves customer data by ID. Admin-only operation."
    )
    @GetMapping("/{customerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Customer> getCustomerById(
        @PathVariable Long customerId
    ) {

        LOGGER.info(LogMarkers.LOG,
            "GET_CUSTOMER_BY_ID | admin={} customerId={}",
            authUserProvider.getLogin(), customerId
        );

        return ResponseEntity.ok(
            customerService.getCustomerById(customerId)
        );
    }

    @Operation(
        summary = "List customers",
        description = "Returns a paginated list of customers. Admin-only operation."
    )
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Customer>> listCustomers(
        @RequestParam(required = false) Status status,
        @RequestParam(defaultValue = "0") int page
    ) {

        Pageable pageable = PageRequest.of(
            page,
            defaultPageSize,
            Sort.by("createdAt").ascending()
        );

        return ResponseEntity.ok(
            customerService.filterByStatus(status, pageable)
        );
    }
}
