package com.guga.walletserviceapi.controller;

import java.net.URI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.guga.walletserviceapi.audit.AuditLogContext;
import com.guga.walletserviceapi.audit.AuditLogger;
import com.guga.walletserviceapi.dto.transaction.TransactionDepositDTO;
import com.guga.walletserviceapi.dto.transaction.TransactionResponseDTO;
import com.guga.walletserviceapi.dto.transaction.TransactionTransferDTO;
import com.guga.walletserviceapi.dto.transaction.TransactionWithdrawDTO;
import com.guga.walletserviceapi.exception.ResourceBadRequestException;
import com.guga.walletserviceapi.helpers.GlobalHelper;
import com.guga.walletserviceapi.logging.LogMarkers;
import com.guga.walletserviceapi.model.DepositMoney;
import com.guga.walletserviceapi.model.Transaction;
import com.guga.walletserviceapi.model.TransferMoneyReceived;
import com.guga.walletserviceapi.model.TransferMoneySend;
import com.guga.walletserviceapi.model.enums.OperationType;
import com.guga.walletserviceapi.model.enums.StatusTransaction;
import com.guga.walletserviceapi.security.auth.JwtAuthenticatedUserProvider;
import com.guga.walletserviceapi.service.TransactionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/transactions")
@Tag(name = "Transaction", description = "Endpoints for managing transactions")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class TransactionController {

    private static final Logger LOGGER = LogManager.getLogger(TransactionController.class);

    private final TransactionService transactionService;
    private final JwtAuthenticatedUserProvider authUserProvider;

    // =====================================================
    // USER CONTEXT - CREATION
    // =====================================================

    @Operation(
        summary = "Deposit money",
        description = "Creates a DEPOSIT transaction for the authenticated user's wallet."
    )
    @PostMapping("/deposit")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TransactionResponseDTO> createDeposit(
        @RequestBody @Valid TransactionDepositDTO dto
    ) {
        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());
        Long walletId = auditCtx.getWalletId();

        LOGGER.info(LogMarkers.LOG, "DEPOSIT | walletId={} amount={}", walletId, dto.amount());
        AuditLogger.log("TRANSACTION_DEPOSIT [START]", auditCtx);

        DepositMoney deposit = transactionService.saveDepositMoney(
            walletId, dto.amount(), dto.cpfSender(), dto.terminalId(), dto.senderName()
        );

        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{transactionId}")
            .buildAndExpand(deposit.getTransactionId())
            .toUri();

        AuditLogger.log("TRANSACTION_DEPOSIT [SUCCESS]", auditCtx);
        return ResponseEntity.created(location).body(toDto(deposit));
    }

    @Operation(
        summary = "Withdraw money",
        description = "Creates a WITHDRAW transaction for the authenticated user's wallet."
    )
    @PostMapping("/withdraw")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TransactionResponseDTO> createWithdraw(
        @RequestBody @Valid TransactionWithdrawDTO dto
    ) {
        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());
        Long walletId = auditCtx.getWalletId();

        LOGGER.info(LogMarkers.LOG, "WITHDRAW | walletId={} amount={}", walletId, dto.amount());
        AuditLogger.log("TRANSACTION_WITHDRAW [START]", auditCtx);

        Transaction transaction = transactionService.saveWithdrawMoney(walletId, dto.amount());

        AuditLogger.log("TRANSACTION_WITHDRAW [SUCCESS]", auditCtx);
        return ResponseEntity.ok(toDto(transaction));
    }

    @Operation(
        summary = "Transfer money",
        description = "Creates a TRANSFER transaction from the authenticated user's wallet."
    )
    @PostMapping("/transfer")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TransactionResponseDTO> createTransfer(
        @RequestBody @Valid TransactionTransferDTO dto
    ) {
        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());
        Long walletIdSend = auditCtx.getWalletId();

        LOGGER.info(LogMarkers.LOG, "TRANSFER | fromWallet={} toWallet={} amount={}", walletIdSend, dto.walletIdReceived(), dto.amount());
        AuditLogger.log("TRANSACTION_TRANSFER [START]", auditCtx);

        TransferMoneySend transfer = transactionService.saveTransferMoneySend(walletIdSend, dto.walletIdReceived(), dto.amount());

        AuditLogger.log("TRANSACTION_TRANSFER [SUCCESS]", auditCtx);
        return ResponseEntity.ok(toDto(transfer));
    }

    // =====================================================
    // USER CONTEXT - READ / EXTRACTS
    // =====================================================

    @Operation(summary = "Get details of my own transaction")
    @GetMapping("/me/{transactionId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TransactionResponseDTO> getMyTransactionById(@PathVariable Long transactionId) {
        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());
        Transaction transaction = transactionService.getTransactionById(transactionId);

        // REGRA DE SEGURANÇA 1: Validar se a transação pertence ao usuário logado
        if (!transaction.getWalletId().equals(auditCtx.getWalletId())) {
            LOGGER.warn("SECURITY ALERT | Unauthorized access attempt | userWallet={} targetTransactionId={}", 
                        auditCtx.getWalletId(), transactionId);
            throw new ResourceBadRequestException("Access denied: This transaction does not belong to your wallet.");
        }

        AuditLogger.log("TRANSACTION_GET_BY_ID_ME", auditCtx);
        return ResponseEntity.ok(toDto(transaction));
    }

    @Operation(
        summary = "List my transactions",
        description = "Retrieves transactions for the authenticated user's wallet."
    )
    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<TransactionResponseDTO>> listMyTransactions(
        @RequestParam(required = false) StatusTransaction status
        //@RequestParam(defaultValue = "0") int page
    ) {
        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());
        Long walletId = auditCtx.getWalletId();

        Pageable pageable = GlobalHelper.getDefaultPageable();

        AuditLogger.log("TRANSACTION_LIST_ME", auditCtx);
        return ResponseEntity.ok(
            transactionService.filterTransactionByWalletIdAndProcessType(walletId, status, pageable).map(this::toDto)
        );
    }

    @Operation(summary = "List my last 150 deposits")
    @GetMapping("/me/deposits")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<TransactionResponseDTO>> listMyDeposits() {
        return listMyTransactionsByOperation(OperationType.DEPOSIT);
    }

    @Operation(summary = "List my last 150 withdraws")
    @GetMapping("/me/withdraws")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<TransactionResponseDTO>> listMyWithdraws() {
        return listMyTransactionsByOperation(OperationType.WITHDRAW);
    }

    @Operation(summary = "List my last 150 sent transfers")
    @GetMapping("/me/transfers-send")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<TransactionResponseDTO>> listMyTransfersSent() {
        return listMyTransactionsByOperation(OperationType.TRANSFER_SEND);
    }

    @Operation(summary = "List my last 150 received transfers")
    @GetMapping("/me/transfers-received")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<TransactionResponseDTO>> listMyTransfersReceived() {
        return listMyTransactionsByOperation(OperationType.TRANSFER_RECEIVED);
    }

    private ResponseEntity<Page<TransactionResponseDTO>> listMyTransactionsByOperation(OperationType operation) {
        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());
        Long walletId = auditCtx.getWalletId();

        Pageable limitPageable = PageRequest.of(0, 150, Sort.by(Sort.Direction.DESC, "createdAt", "transactionId"));
        LOGGER.info(LogMarkers.LOG, "LIST_MY_TRANSACTIONS | walletId={} operation={} limit=150", walletId, operation);

        String auditTag = "TRANSACTION_LIST_" + operation.name() + "_ME";
        AuditLogger.log(auditTag, auditCtx);

        return ResponseEntity.ok(
            transactionService.filterTransactionByWalletIdAndOperationType(walletId, operation, limitPageable).map(this::toDto)
        );
    }

    // =====================================================
    // ADMIN CONTEXT
    // =====================================================

    @Operation(
        summary = "Get transaction by ID (ADMIN)",
        description = "Retrieves a transaction by ID. Admin-only operation."
    )
    @GetMapping("/{transactionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TransactionResponseDTO> getTransactionById(@PathVariable Long transactionId) {
        return ResponseEntity.ok(toDto(transactionService.getTransactionById(transactionId)));
    }

    @Operation(
        summary = "List transactions by wallet (ADMIN)",
        description = "Retrieves transactions by wallet ID. Admin-only operation."
    )
    @GetMapping("/by-wallet/{walletId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<TransactionResponseDTO>> listTransactionsByWallet(
        @PathVariable Long walletId,
        @RequestParam(required = false) StatusTransaction type
        //@RequestParam(defaultValue = "0") int page
    ) {
        Pageable pageable = GlobalHelper.getDefaultPageable();

        return ResponseEntity.ok(
            transactionService.filterTransactionByWalletIdAndProcessType(walletId, type, pageable).map(this::toDto)
        );
    }

    // =====================================================
    // MAPPERS (PRIVATE)
    // =====================================================

    private TransactionResponseDTO toDto(Transaction entity) {
        if (entity == null) return null;
        
        String senderName = null;
        String cpfSender = null;

        if (entity instanceof DepositMoney d) {
            if (d.getDepositSender() == null) return null;
            senderName = d.getDepositSender().getFullName();
            cpfSender = d.getDepositSender().getCpf();
        }
        
        Long relatedWalletId = null;
        if (entity instanceof TransferMoneySend t) {
            relatedWalletId = t.getWalletId(); // .getWalletIdReceived();
        } else if (entity instanceof TransferMoneyReceived r) {
            relatedWalletId = r.getWalletId();
        }

        return new TransactionResponseDTO(
            entity.getTransactionId(), entity.getWalletId(), entity.getAmount(),
            entity.getOperationType(), entity.getStatusTransaction(), entity.getCreatedAt(),
            senderName, cpfSender, relatedWalletId
        );
    }
}