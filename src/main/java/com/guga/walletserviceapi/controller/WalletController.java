package com.guga.walletserviceapi.controller;

import java.net.URI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
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
import com.guga.walletserviceapi.logging.LogMarkers;
import com.guga.walletserviceapi.model.Wallet;
import com.guga.walletserviceapi.model.enums.Status;
import com.guga.walletserviceapi.security.auth.JwtAuthenticatedUserProvider;
import com.guga.walletserviceapi.service.WalletService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("${controller.path.base}/wallets")
@Tag(name = "Wallet", description = "Endpoints for managing wallets")
@SecurityRequirement(name = "bearerAuth")
public class WalletController {
    
    private static final Logger LOGGER = LogManager.getLogger(WalletController.class);

    @Autowired
    private WalletService walletService;

    @Autowired
    private JwtAuthenticatedUserProvider authUserProvider;

    @Value("${spring.data.web.pageable.default-page-size}")
    private int defaultPageSize;

    // ============================
    // CREATE WALLET
    // ============================
    @Operation(
        summary = "Create a new Wallet",
        description = "Creates a new Wallet with the data provided in the request body."
    )
    @PostMapping("/wallet")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Wallet> createWallet(
            @RequestBody @Valid Wallet wallet
        ) 
    {

        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());

        LOGGER.info(LogMarkers.LOG, "Creating wallet | user={}", auditCtx.getUsername());
        AuditLogger.log("WALLET_CREATE [START]", auditCtx);

        Wallet createdWallet = walletService.saveWallet(wallet);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdWallet.getWalletId())
                .toUri();

        LOGGER.info("Wallet created successfully | walletId={}", createdWallet.getWalletId());
        AuditLogger.log("WALLET_CREATE [SUCCESS]", auditCtx);

        return ResponseEntity.created(location).body(createdWallet);
    }

    // ============================
    // GET WALLET BY ID
    // ============================
    @Operation(
        summary = "Get Wallet by ID",
        description = "Retrieves a Wallet by their ID."
    )
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Wallet> getWalletById(
            @PathVariable Long walletId
    ) {

        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());

        LOGGER.info(LogMarkers.LOG, "Fetching wallet | walletId={}", walletId);
        AuditLogger.log("WALLET_GET_BY_ID", auditCtx);

        Wallet wallet = walletService.getWalletById(walletId);

        return new ResponseEntity<>(wallet, HttpStatus.OK);
    }

    // ============================
    // UPDATE WALLET
    // ============================
    @Operation(
        summary = "Update Wallet by ID",
        description = "Updates a Wallet by their ID."
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Wallet> updateWallet(
            @PathVariable Long walletId,
            @RequestBody @Valid Wallet walletUpdate
    ) {

        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());

        LOGGER.info("Updating wallet | walletId={}", walletId);
        AuditLogger.log("WALLET_UPDATE [START]", auditCtx);

        Wallet wallet = walletService.updateWallet(walletId, walletUpdate);

        AuditLogger.log("WALLET_UPDATE [SUCCESS]", auditCtx);
        return new ResponseEntity<>(wallet, HttpStatus.OK);
    }

    // ============================
    // LIST ALL WALLETS
    // ============================
    @Operation(
        summary = "Get all Wallets",
        description = "Retrieves all Wallets."
    )
    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Wallet>> getAllWallets(
        @RequestParam(required = false) Status status,
        @RequestParam(defaultValue = "0") int page
        ) 
    {

        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());

        LOGGER.info("Listing wallets | status={} page={}", status, page);
        AuditLogger.log("WALLET_LIST_ALL", auditCtx);

        Pageable pageable = PageRequest.of(
                page,
                defaultPageSize,
                Sort.by(
                    Sort.Order.asc("status"),
                    Sort.Order.asc("createdAt")
                )
        );

        Page<Wallet> pageWallet = walletService.getAllWallets(status, pageable);

        return new ResponseEntity<>(pageWallet, HttpStatus.OK);
    }

    // ============================
    // SEARCH BY CUSTOMER
    // ============================
    @Operation(
        summary = "Get Wallets by Customer ID",
        description = "Retrieves Wallets by Customer ID."
    )
    @GetMapping("/search-by-customer")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<Wallet>> getWalletByCustomerId(
            @RequestParam Long customerId,
            @RequestParam(defaultValue = "0") int page
    ) {

        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());

        LOGGER.info("Searching wallets by customer | customerId={}", customerId);
        AuditLogger.log("WALLET_SEARCH_BY_CUSTOMER [GET]", auditCtx);

        Pageable pageable = PageRequest.of(
                page,
                defaultPageSize,
                Sort.by(
                        Sort.Order.asc("customerId"),
                        Sort.Order.asc("status"),
                        Sort.Order.asc("createdAt")
                )
        );

        Page<Wallet> findResult =
                walletService.getWalletByCustomerId(customerId, pageable);

        return new ResponseEntity<>(findResult, HttpStatus.OK);
    }
}
