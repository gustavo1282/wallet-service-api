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
@Tag(name = "Customer", description = "Customer domain operations")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class CustomerController {

    private static final Logger LOGGER = LogManager.getLogger(CustomerController.class);

    private final CustomerService customerService;
    private final JwtAuthenticatedUserProvider authUserProvider;

    // =====================================================
    // USER CONTEXT
    // =====================================================

    @Operation(
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
            toDto(customerService.getCustomerById(customerId))
        );
    }

    @Operation(
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

        Customer customerUpdate = toEntity(dto);

        // 2. Chama o serviço passando o ID extraído do token
        return ResponseEntity.ok(
            toDto(customerService.updateCustomer(customerId, customerUpdate))
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
    public ResponseEntity<CustomerResponseDTO> createCustomer(
        @RequestBody @Valid CustomerCreateDTO dto
    ) {

        AuditLogContext auditContext = AuditLogContext.from(authUserProvider.get());

        LOGGER.info(LogMarkers.LOG,
            "CREATE_CUSTOMER | admin={}",
            auditContext.getUsername()
        );

        Customer customer = toEntity(dto);
        Customer createdCustomer = customerService.saveCustomer(customer);

        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{customerId}")
            .buildAndExpand(createdCustomer.getCustomerId())
            .toUri();

        return ResponseEntity
            .created(location)
            .header("X-Trace-Id", auditContext.getTraceId())
            .body(toDto(createdCustomer));
    }

    @Operation(
        summary = "Update customer by ID",
        description = "Updates an existing customer by ID. Admin-only operation."
    )
    @PutMapping("/{customerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomerResponseDTO> updateCustomer(
        @PathVariable Long customerId,
        @RequestBody @Valid CustomerUpdateDTO dto
    ) {

        LOGGER.info(LogMarkers.LOG, "UPDATE_CUSTOMER | admin={} customerId={}",
            authUserProvider.getLogin(), customerId
        );

        Customer customerUpdate = toEntity(dto);
        return ResponseEntity.ok(toDto(customerService.updateCustomer(customerId, customerUpdate)));

    }

    @Operation(
        summary = "Get customer by ID",
        description = "Retrieves customer data by ID. Admin-only operation."
    )
    @GetMapping("/{customerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomerResponseDTO> getCustomerById(
        @PathVariable Long customerId
    ) {

        LOGGER.info(LogMarkers.LOG,
            "GET_CUSTOMER_BY_ID | admin={} customerId={}",
            authUserProvider.getLogin(), customerId
        );

        return ResponseEntity.ok(
            toDto(customerService.getCustomerById(customerId))
        );
    }

    @Operation(
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
        return ResponseEntity.ok(
            customerService.filterByStatus(status, pageable).map(this::toDto)
        );
    }

    // =====================================================
    // MAPPERS (PRIVATE)
    // =====================================================

    private CustomerResponseDTO toDto(Customer entity) {
        if (entity == null) return null;
        return new CustomerResponseDTO(
            entity.getCustomerId(),
            entity.getFirstName(),
            entity.getLastName(),
            entity.getFullName(), // FullName derived
            entity.getEmail(),
            entity.getCpf(),
            entity.getPhoneNumber(),
            entity.getDocumentId(),
            entity.getBirthDate(),
            entity.getStatus()
        );
    }

    private Customer toEntity(CustomerCreateDTO dto) {
        Customer customer = new Customer();
        customer.setFirstName(dto.firstName());
        customer.setLastName(dto.lastName());
        customer.setEmail(dto.email());
        customer.setCpf(dto.cpf());
        customer.setPhoneNumber(dto.phoneNumber());
        customer.setDocumentId(dto.documentId());
        customer.setBirthDate(dto.birthDate());
        return customer;
    }

    private Customer toEntity(CustomerUpdateDTO dto) {
        Customer customer = new Customer();
        customer.setStatus(dto.status());
        customer.setPhoneNumber(dto.phoneNumber());
        customer.setEmail(dto.email());
        // customer.setDocumentId(dto.documentId());
        // customer.setFirstName(dto.firstName());
        // customer.setLastName(dto.lastName());
        // customer.setBirthDate(dto.birthDate());
        return customer;
    }
}
