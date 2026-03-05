package com.guga.walletserviceapi.controller;

import java.net.URI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import com.guga.walletserviceapi.audit.AuditLogger;
import com.guga.walletserviceapi.dto.customer.CustomerCreateDTO;
import com.guga.walletserviceapi.dto.customer.CustomerMapper;
import com.guga.walletserviceapi.dto.customer.CustomerResponseDTO;
import com.guga.walletserviceapi.dto.customer.CustomerUpdateDTO;
import com.guga.walletserviceapi.helpers.GlobalHelper;
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
@RequestMapping("/customers")
@Tag(name = "Customers", description = "Customer domain operations")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class CustomerController {

    private static final Logger LOGGER = LogManager.getLogger(CustomerController.class);

    private final CustomerService customerService;
    private final JwtAuthenticatedUserProvider authUserProvider;
    private final CustomerMapper customerMapper;

    // =====================================================
    // USER CONTEXT
    // =====================================================

    @Operation(
        operationId = "cust_01_get_customer_me",
        summary = "Get authenticated customer profile",
        description = "Returns customer data associated with the authenticated user (JWT context)."
    )
    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CustomerResponseDTO> getMyCustomer() {

        JwtAuthenticationDetails authDetails = authUserProvider.get();
        Long customerId = authDetails.getCustomerId();

        LOGGER.info(LogMarkers.LOG,
            "GET_CUSTOMER_ME | customerId={} login={}", customerId, authDetails.getLogin()
        );

        return ResponseEntity.ok(
            customerMapper.toDto(customerService.getCustomerById(customerId))
        );
    }

    @Operation(
        operationId = "cust_02_update_customer_me",
        summary = "Update my customer data",
        description = "Updates the authenticated user's customer data."
    )
    @PutMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CustomerResponseDTO> updateMyCustomer(
        @RequestBody @Valid CustomerUpdateDTO dto
    ) {
        // 1. Extrai o ID do cliente diretamente do contexto de segurança (Token)
        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());
        Long customerId = auditCtx.getCustomerId();

        LOGGER.info(LogMarkers.LOG, "UPDATE_MY_CUSTOMER | customerId={} user={}",
            customerId, auditCtx.getUsername()
        );

        AuditLogger.log("CUSTOMER_UPDATE_ME", auditCtx);

        Customer customerUpdate = customerMapper.toEntity(dto);
        // Customer customerUpdate = customerMapper.toEntity(dto); // Será usado após a criação do mapper
        // 2. Chama o serviço passando o ID extraído do token
        return ResponseEntity.ok(
            customerMapper.toDto(customerService.updateCustomer(customerId, customerUpdate))
        );
    }

    // =====================================================
    // ADMIN CONTEXT
    // =====================================================

    @Operation(
        operationId = "cust_03_list_customers",
        summary = "List customers",
        description = "Returns a paginated list of customers. Admin-only operation."
    )
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<CustomerResponseDTO>> listCustomers(
        @RequestParam(required = false) Status status
        //@RequestParam(defaultValue = "0") int page
    ) {
        Pageable pageable = GlobalHelper.getDefaultPageable();
        return ResponseEntity.ok(customerService.filterByStatus(status, pageable).map(customerMapper::toDto));
    }


    @Operation(
        operationId = "cust_04_create_customer",
        summary = "Create a new customer",
        description = "Creates a new customer. Admin-only operation."
    )
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomerResponseDTO> createCustomer(
        @RequestBody @Valid CustomerCreateDTO dto
    ) {

        AuditLogContext auditContext = AuditLogContext.from(authUserProvider.get());

        LOGGER.info(LogMarkers.LOG,
            "CREATE_CUSTOMER | admin={}",
            auditContext.getUsername()
        );

        Customer customer = customerMapper.toEntity(dto);
        Customer createdCustomer = customerService.saveCustomer(customer);

        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{customerId}")
            .buildAndExpand(createdCustomer.getCustomerId())
            .toUri();

        return ResponseEntity
            .created(location)
            .header("X-Trace-Id", auditContext.getTraceId()) // TODO: Verificar se o traceId está sendo gerado corretamente
            .body(customerMapper.toDto(createdCustomer));
    }


    @Operation(
        operationId = "cust_05_get_customer",
        summary = "Get customer by ID",
        description = "Retrieves customer data by ID. Admin-only operation."
    )
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomerResponseDTO> getCustomerById(
        @PathVariable Long id
        ) 
    {

        LOGGER.info(LogMarkers.LOG,
            "GET_CUSTOMER_BY_ID | admin={} customerId={}",
            authUserProvider.getLogin(), id
        );

        return ResponseEntity.ok(
            customerMapper.toDto(customerService.getCustomerById(id))
        );
    }

    @Operation(
        operationId = "cust_06_update_customer",
        summary = "Update customer by ID",
        description = "Updates an existing customer by ID. Admin-only operation."
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomerResponseDTO> updateCustomer(
        @PathVariable Long id,
        @RequestBody @Valid CustomerUpdateDTO dto
    ) {

        LOGGER.info(LogMarkers.LOG, "UPDATE_CUSTOMER | admin={} customerId={}",
            authUserProvider.getLogin(), id
        );

        Customer customerUpdate = customerMapper.toEntity(dto);
        return ResponseEntity.ok(customerMapper.toDto(customerService.updateCustomer(id, customerUpdate)));

    }

}
