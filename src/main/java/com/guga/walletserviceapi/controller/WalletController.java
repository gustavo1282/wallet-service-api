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
import com.guga.walletserviceapi.dto.wallet.WalletCreateDTO;
import com.guga.walletserviceapi.dto.wallet.WalletMapper;
import com.guga.walletserviceapi.dto.wallet.WalletResponseDTO;
import com.guga.walletserviceapi.dto.wallet.WalletUpdateDTO;
import com.guga.walletserviceapi.helpers.GlobalHelper;
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
@RequestMapping("/wallets")
@Tag(name = "Wallet", description = "Endpoints for managing wallets")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class WalletController {

    private static final Logger LOGGER = LogManager.getLogger(WalletController.class);

    private final WalletService walletService;
    private final JwtAuthenticatedUserProvider authUserProvider;
    private final WalletMapper walletMapper;

    // =====================================================
    // USER CONTEXT
    // =====================================================

    @Operation(
        operationId = "wallet_01_get_my_wallet",
        summary = "Get authenticated user's wallet",
        description = "Returns the wallet associated with the authenticated user (JWT context)."
    )
    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<WalletResponseDTO> getMyWallet() {

        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());
        Long walletId = auditCtx.getWalletId();

        LOGGER.info(LogMarkers.LOG,
            "GET_MY_WALLET | walletId={} user={}",
            walletId, auditCtx.getUsername()
        );

        AuditLogger.log(
            "WALLET_GET_ME",
            auditCtx.toBuilder().info("walletId=" + walletId).build()
        );

        return ResponseEntity.ok(
            walletMapper.toDto(walletService.getWalletById(walletId))
        );
    }

    // =====================================================
    // USER CONTEXT - MY WALLETS
    // =====================================================

    @Operation(
        operationId = "wallet_02_list_my_wallets",
        summary = "List all wallets of the authenticated customer",
        description = "Returns a paginated list of all wallets belonging to the customer identified by the JWT token."
    )
    @GetMapping("/me/list")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<WalletResponseDTO>> getMyWalletsList() {

        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());
        Long customerId = auditCtx.getCustomerId();

        LOGGER.info(LogMarkers.LOG, "GET_MY_WALLETS | customerId={} user={}",
            customerId, auditCtx.getUsername()
        );

        Pageable pageable = GlobalHelper.getDefaultPageable();
        Page<WalletResponseDTO> result = walletService
            .getWalletByCustomerId(customerId, pageable)
            .map(walletMapper::toDto);
        AuditLogger.log(
            "WALLET_GET_MY_LIST",
            auditCtx.toBuilder().info("rows=" + result.getNumberOfElements()).build()
        );
        return ResponseEntity.ok(result);
    }

    @Operation(
        operationId = "wallet_03_update_my_wallet",
        summary = "Update authenticated user's wallet",
        description = "Updates the wallet associated with the authenticated user (JWT context)."
    )
    @PutMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<WalletResponseDTO> updateMyWallet(
        @RequestBody @Valid WalletUpdateDTO dto
    ) {

        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());
        Long walletId = auditCtx.getWalletId();

        LOGGER.info(LogMarkers.LOG, "UPDATE_MY_WALLET | walletId={} user={}",
            walletId, auditCtx.getUsername()
        );

        AuditLogger.log("WALLET_UPDATE_ME [START]", auditCtx);

        Wallet walletUpdate = walletMapper.toEntity(dto);
        Wallet updatedWallet = walletService.updateWallet(walletId, walletUpdate);

        AuditLogger.log(
            "WALLET_UPDATE_ME [SUCCESS]",
            auditCtx.toBuilder().info("walletId=" + updatedWallet.getWalletId()).build()
        );

        return ResponseEntity.ok(walletMapper.toDto(updatedWallet));
    }


    @Operation(
        operationId = "wallet_04_list",
        summary = "List wallets",
        description = "Retrieves all wallets with optional status filter. Admin-only operation."
    )
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<WalletResponseDTO>> listWallets(
        @RequestParam(required = false) Status status
    ) {

        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());

        LOGGER.info(LogMarkers.LOG,"LIST_WALLETS | status={} admin={}",
            status, auditCtx.getUsername()
        );

        Pageable pageable = GlobalHelper.getDefaultPageable();
        Page<WalletResponseDTO> result = walletService
            .getAllWallets(status, pageable)
            .map(walletMapper::toDto);
        AuditLogger.log(
            "WALLET_LIST",
            auditCtx.toBuilder().info("rows=" + result.getNumberOfElements()).build()
        );
        return ResponseEntity.ok(result);
    }



    // =====================================================
    // CREATE WALLET (ADMIN)
    // =====================================================

    @Operation(
        operationId = "wallet_05_create",
        summary = "Create wallet",
        description = "Creates a new wallet. Admin-only operation."
    )
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WalletResponseDTO> createWallet(
        @RequestBody @Valid WalletCreateDTO dto
    ) {

        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());

        LOGGER.info(LogMarkers.LOG,
            "CREATE_WALLET | admin={}",
            auditCtx.getUsername()
        );

        AuditLogger.log("WALLET_CREATE [START]", auditCtx);

        Wallet wallet = walletMapper.toEntity(dto);
        Wallet createdWallet = walletService.saveWallet(wallet);

        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{walletId}")
            .buildAndExpand(createdWallet.getWalletId())
            .toUri();

        AuditLogger.log(
            "WALLET_CREATE [SUCCESS]",
            auditCtx.toBuilder().info("walletId=" + createdWallet.getWalletId()).build()
        );

        return ResponseEntity.created(location).body(walletMapper.toDto(createdWallet));
    }

    // =====================================================
    // ADMIN CONTEXT
    // =====================================================

    @Operation(
        operationId = "wallet_06_get_by_id",
        summary = "Get wallet by ID",
        description = "Retrieves a wallet by ID. Admin-only operation."
    )
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WalletResponseDTO> getWalletById(@PathVariable Long id) {

        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());

        LOGGER.info(LogMarkers.LOG,
            "GET_WALLET_BY_ID | walletId={} admin={}",
            id, auditCtx.getUsername()
        );

        AuditLogger.log(
            "WALLET_GET_BY_ID",
            auditCtx.toBuilder().info("walletId=" + id).build()
        );

        return ResponseEntity.ok(
            walletMapper.toDto(walletService.getWalletById(id))
        );
    }

    @Operation(
        operationId = "wallet_07_update_by_id",
        summary = "Update wallet",
        description = "Updates a wallet by ID. Admin-only operation."
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WalletResponseDTO> updateWallet(
        @PathVariable("id") Long walletId,
        @RequestBody @Valid WalletUpdateDTO dto
    ) {

        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());

        LOGGER.info(LogMarkers.LOG,
            "UPDATE_WALLET | walletId={} admin={}",
            walletId, auditCtx.getUsername()
        );

        AuditLogger.log("WALLET_UPDATE [START]", auditCtx);

        Wallet walletUpdate = walletMapper.toEntity(dto);
        Wallet wallet = walletService.updateWallet(walletId, walletUpdate);

        AuditLogger.log(
            "WALLET_UPDATE [SUCCESS]",
            auditCtx.toBuilder().info("walletId=" + wallet.getWalletId()).build()
        );

        return ResponseEntity.ok(walletMapper.toDto(wallet));
    }

    @Operation(
        operationId = "wallet_08_list_by_customer",
        summary = "Get wallets by customer",
        description = "Retrieves wallets by customer ID. Admin-only operation."
    )
    @GetMapping("/customer/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<WalletResponseDTO>> getWalletsByCustomer(
        @PathVariable Long id
        ) 
    {

        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());

        LOGGER.info(LogMarkers.LOG,
            "GET_WALLETS_BY_CUSTOMER | customerId={} admin={}",
            id, auditCtx.getUsername()
        );

        Pageable pageable = GlobalHelper.getDefaultPageable();
        Page<WalletResponseDTO> result = walletService
            .getWalletByCustomerId(id, pageable)
            .map(walletMapper::toDto);
        AuditLogger.log(
            "WALLET_GET_BY_CUSTOMER",
            auditCtx.toBuilder().info("customerId=" + id + ",rows=" + result.getNumberOfElements()).build()
        );
        return ResponseEntity.ok(result);
    }

}
