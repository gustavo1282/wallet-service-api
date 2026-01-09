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
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("${controller.path.base}/wallets")
@Tag(name = "Wallet", description = "Endpoints for managing wallets")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class WalletController {

    private static final Logger LOGGER = LogManager.getLogger(WalletController.class);

    private final WalletService walletService;

    private final JwtAuthenticatedUserProvider authUserProvider;

    @Value("${spring.data.web.pageable.default-page-size}")
    private int defaultPageSize;

    // =====================================================
    // USER CONTEXT
    // =====================================================

    @Operation(
        summary = "Get authenticated user's wallet",
        description = "Returns the wallet associated with the authenticated user (JWT context)."
    )
    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Wallet> getMyWallet() {

        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());
        Long walletId = auditCtx.getWalletId();

        LOGGER.info(LogMarkers.LOG,
            "GET_MY_WALLET | walletId={} user={}",
            walletId, auditCtx.getUsername()
        );

        AuditLogger.log("WALLET_GET_ME", auditCtx);

        return ResponseEntity.ok(
            walletService.getWalletById(walletId)
        );
    }

    // =====================================================
    // CREATE WALLET (ADMIN)
    // =====================================================

    @Operation(
        summary = "Create wallet",
        description = "Creates a new wallet. Admin-only operation."
    )
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Wallet> createWallet(
        @RequestBody @Valid Wallet wallet
    ) {

        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());

        LOGGER.info(LogMarkers.LOG,
            "CREATE_WALLET | admin={}",
            auditCtx.getUsername()
        );

        AuditLogger.log("WALLET_CREATE [START]", auditCtx);

        Wallet createdWallet = walletService.saveWallet(wallet);

        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{walletId}")
            .buildAndExpand(createdWallet.getWalletId())
            .toUri();

        AuditLogger.log("WALLET_CREATE [SUCCESS]", auditCtx);

        return ResponseEntity.created(location).body(createdWallet);
    }

    // =====================================================
    // ADMIN CONTEXT
    // =====================================================

    @Operation(
        summary = "Get wallet by ID",
        description = "Retrieves a wallet by ID. Admin-only operation."
    )
    @GetMapping("/{walletId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Wallet> getWalletById(
        @PathVariable Long walletId
    ) {

        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());

        LOGGER.info(LogMarkers.LOG,
            "GET_WALLET_BY_ID | walletId={} admin={}",
            walletId, auditCtx.getUsername()
        );

        AuditLogger.log("WALLET_GET_BY_ID", auditCtx);

        return ResponseEntity.ok(
            walletService.getWalletById(walletId)
        );
    }

    @Operation(
        summary = "Update wallet",
        description = "Updates a wallet by ID. Admin-only operation."
    )
    @PutMapping("/{walletId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Wallet> updateWallet(
        @PathVariable Long walletId,
        @RequestBody @Valid Wallet walletUpdate
    ) {

        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());

        LOGGER.info(LogMarkers.LOG,
            "UPDATE_WALLET | walletId={} admin={}",
            walletId, auditCtx.getUsername()
        );

        AuditLogger.log("WALLET_UPDATE [START]", auditCtx);

        Wallet wallet = walletService.updateWallet(walletId, walletUpdate);

        AuditLogger.log("WALLET_UPDATE [SUCCESS]", auditCtx);

        return ResponseEntity.ok(wallet);
    }

    @Operation(
        summary = "List wallets",
        description = "Retrieves all wallets with optional status filter. Admin-only operation."
    )
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Wallet>> listWallets(
        @RequestParam(required = false) Status status,
        @RequestParam(defaultValue = "0") int page
    ) {

        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());

        LOGGER.info(LogMarkers.LOG,
            "LIST_WALLETS | status={} page={} admin={}",
            status, page, auditCtx.getUsername()
        );

        AuditLogger.log("WALLET_LIST", auditCtx);

        Pageable pageable = PageRequest.of(
            page,
            defaultPageSize,
            Sort.by(
                Sort.Order.asc("status"),
                Sort.Order.asc("createdAt")
            )
        );

        return ResponseEntity.ok(
            walletService.getAllWallets(status, pageable)
        );
    }

    @Operation(
        summary = "Get wallets by customer",
        description = "Retrieves wallets by customer ID. Admin-only operation."
    )
    @GetMapping("/by-customer/{customerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Wallet>> getWalletsByCustomer(
        @PathVariable Long customerId,
        @RequestParam(defaultValue = "0") int page
    ) {

        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());

        LOGGER.info(LogMarkers.LOG,
            "GET_WALLETS_BY_CUSTOMER | customerId={} admin={}",
            customerId, auditCtx.getUsername()
        );

        AuditLogger.log("WALLET_GET_BY_CUSTOMER", auditCtx);

        Pageable pageable = PageRequest.of(
            page,
            defaultPageSize,
            Sort.by(
                Sort.Order.asc("status"),
                Sort.Order.asc("createdAt")
            )
        );

        return ResponseEntity.ok(
            walletService.getWalletByCustomerId(customerId, pageable)
        );
    }
}
