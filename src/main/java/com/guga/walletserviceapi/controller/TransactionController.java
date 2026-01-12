package com.guga.walletserviceapi.controller;

import java.math.BigDecimal;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.guga.walletserviceapi.audit.AuditLogContext;
import com.guga.walletserviceapi.audit.AuditLogger;
import com.guga.walletserviceapi.logging.LogMarkers;
import com.guga.walletserviceapi.model.DepositMoney;
import com.guga.walletserviceapi.model.Transaction;
import com.guga.walletserviceapi.model.TransferMoneySend;
import com.guga.walletserviceapi.model.enums.OperationType;
import com.guga.walletserviceapi.model.enums.StatusTransaction;
import com.guga.walletserviceapi.security.auth.JwtAuthenticatedUserProvider;
import com.guga.walletserviceapi.service.TransactionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("${controller.path.base}/transactions")
@Tag(name = "Transaction", description = "Endpoints for managing transactions")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class TransactionController {

    private static final Logger LOGGER = LogManager.getLogger(TransactionController.class);

    private final TransactionService transactionService;
    private final JwtAuthenticatedUserProvider authUserProvider;

    @Value("${spring.data.web.pageable.default-page-size}")
    private int defaultPageSize;

    // =====================================================
    // USER CONTEXT
    // =====================================================

    @Operation(
        summary = "Deposit money",
        description = "Creates a DEPOSIT transaction for the authenticated user's wallet."
    )
    @PostMapping("/deposit")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<DepositMoney> deposit(
        @RequestParam BigDecimal amount,
        @RequestParam String cpfSender,
        @RequestParam String terminalId,
        @RequestParam String senderName
    ) {

        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());
        Long walletId = auditCtx.getWalletId();

        LOGGER.info(LogMarkers.LOG,
            "DEPOSIT | walletId={} amount={}",
            walletId, amount
        );

        AuditLogger.log("TRANSACTION_DEPOSIT [START]", auditCtx);

        DepositMoney deposit = transactionService.saveDepositMoney(
            walletId, amount, cpfSender, terminalId, senderName
        );

        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{transactionId}")
            .buildAndExpand(deposit.getTransactionId())
            .toUri();

        AuditLogger.log("TRANSACTION_DEPOSIT [SUCCESS]", auditCtx);

        return ResponseEntity.created(location).body(deposit);
    }

    @Operation(
        summary = "Withdraw money",
        description = "Creates a WITHDRAW transaction for the authenticated user's wallet."
    )
    @PostMapping("/withdraw")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Transaction> withdraw(
        @RequestParam BigDecimal amount
    ) {

        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());
        Long walletId = auditCtx.getWalletId();

        LOGGER.info(LogMarkers.LOG,
            "WITHDRAW | walletId={} amount={}",
            walletId, amount
        );

        AuditLogger.log("TRANSACTION_WITHDRAW [START]", auditCtx);

        Transaction transaction = transactionService.saveWithdrawMoney(walletId, amount);

        AuditLogger.log("TRANSACTION_WITHDRAW [SUCCESS]", auditCtx);

        return ResponseEntity.ok(transaction);
    }

    @Operation(
        summary = "Transfer money",
        description = "Creates a TRANSFER transaction from the authenticated user's wallet."
    )
    @PostMapping("/transfer")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TransferMoneySend> transfer(
        @RequestParam Long walletIdReceived,
        @RequestParam BigDecimal amount
    ) {

        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());
        Long walletIdSend = auditCtx.getWalletId();

        LOGGER.info(LogMarkers.LOG,
            "TRANSFER | fromWallet={} toWallet={} amount={}",
            walletIdSend, walletIdReceived, amount
        );

        AuditLogger.log("TRANSACTION_TRANSFER [START]", auditCtx);

        TransferMoneySend transfer = transactionService
            .saveTransferMoneySend(walletIdSend, walletIdReceived, amount);

        AuditLogger.log("TRANSACTION_TRANSFER [SUCCESS]", auditCtx);

        return ResponseEntity.ok(transfer);
    }

    // =====================================================
    // USER CONTEXT - SPECIFIC EXTRACTS (LAST 150)
    // =====================================================


    @Operation(
        summary = "List my transactions",
        description = "Retrieves transactions for the authenticated user's wallet."
    )
    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<Transaction>> listMyTransactions(
        @RequestParam(required = false) StatusTransaction status,
        @RequestParam(defaultValue = "0") int page
    ) {

        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());
        Long walletId = auditCtx.getWalletId();

        Pageable pageable = PageRequest.of(
            page,
            defaultPageSize,
            Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("transactionId")
            )
        );

        AuditLogger.log("TRANSACTION_LIST_ME", auditCtx);

        return ResponseEntity.ok(
            transactionService.filterTransactionByWalletIdAndProcessType(
                walletId, status, pageable
            )
        );
    }

// =====================================================
    // USER CONTEXT - SPECIFIC EXTRACTS (LAST 150)
    // =====================================================

    @Operation(
        summary = "List my last 150 deposits",
        description = "Retrieves the last 150 DEPOSIT operations for the authenticated user's wallet."
    )
    @GetMapping("/me/deposits")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<Transaction>> listMyDeposits() {
        return listMyTransactionsByOperation(OperationType.DEPOSIT);
    }

    @Operation(
        summary = "List my last 150 withdraws",
        description = "Retrieves the last 150 WITHDRAW operations for the authenticated user's wallet."
    )
    @GetMapping("/me/withdraws")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<Transaction>> listMyWithdraws() {
        return listMyTransactionsByOperation(OperationType.WITHDRAW);
    }

    @Operation(summary = "List my last 150 sent transfers")
    @GetMapping("/me/transfers-sent")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<Transaction>> listMyTransfersSent() {
        return listMyTransactionsByOperation(OperationType.TRANSFER_SEND);
    }

    @Operation(summary = "List my last 150 received transfers")
    @GetMapping("/me/transfers-received")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<Transaction>> listMyTransfersReceived() {
        return listMyTransactionsByOperation(OperationType.TRANSFER_RECEIVED);
    }

    /**
     * Método privado centralizado para extratos específicos do contexto /me.
     * A tag de auditoria é gerada dinamicamente baseada na operação.
     */
    private ResponseEntity<Page<Transaction>> listMyTransactionsByOperation(OperationType operation) {
        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());
        Long walletId = auditCtx.getWalletId();

        // Limite fixo de 150 registros para garantir performance
        Pageable limitPageable = PageRequest.of(0, 150, 
            Sort.by(Sort.Direction.DESC, "createdAt", "transactionId")
        );

        LOGGER.info(LogMarkers.LOG,
            "LIST_MY_TRANSACTIONS | walletId={} operation={} limit=150",
            walletId, operation
        );

        // Concatenando conforme sua convenção para manter o padrão no Loki/Auditoria
        String auditTag = "TRANSACTION_LIST_" + operation.name() + "_ME";
        AuditLogger.log(auditTag, auditCtx);

        return ResponseEntity.ok(
            transactionService.filterTransactionByWalletIdAndOperationType(walletId, operation, limitPageable)
        );
    }




    // =====================================================
    // ADMIN CONTEXT
    // =====================================================

    @Operation(
        summary = "Get transaction by ID",
        description = "Retrieves a transaction by ID. Admin-only operation."
    )
    @GetMapping("/{transactionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Transaction> getTransactionById(
        @PathVariable Long transactionId
    ) {

        return ResponseEntity.ok(
            transactionService.getTransactionById(transactionId)
        );
    }

    @Operation(
        summary = "List transactions by wallet",
        description = "Retrieves transactions by wallet ID. Admin-only operation."
    )
    @GetMapping("/by-wallet/{walletId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Transaction>> listTransactionsByWallet(
        @PathVariable Long walletId,
        @RequestParam(required = false) StatusTransaction type,
        @RequestParam(defaultValue = "0") int page
    ) {

        Pageable pageable = PageRequest.of(
            page,
            defaultPageSize,
            Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("transactionId")
            )
        );

        return ResponseEntity.ok(
            transactionService.filterTransactionByWalletIdAndProcessType(
                walletId, type, pageable
            )
        );
    }
}
