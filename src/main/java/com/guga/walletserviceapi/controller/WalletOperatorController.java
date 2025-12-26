package com.guga.walletserviceapi.controller;

import java.net.URI;
import java.time.LocalDateTime;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.guga.walletserviceapi.model.DepositMoney;
import com.guga.walletserviceapi.model.FileUploadWrapper;
import com.guga.walletserviceapi.model.Transaction;
import com.guga.walletserviceapi.model.TransferMoneySend;
import com.guga.walletserviceapi.model.Wallet;
import com.guga.walletserviceapi.model.WithdrawMoney;
import com.guga.walletserviceapi.model.request.CreateDepositRequest;
import com.guga.walletserviceapi.model.request.CreateTransferRequest;
import com.guga.walletserviceapi.model.request.CreateWithdrawRequest;
import com.guga.walletserviceapi.service.CustomerService;
import com.guga.walletserviceapi.service.DepositSenderService;
import com.guga.walletserviceapi.service.MovementTransactionService;
import com.guga.walletserviceapi.service.TransactionService;
import com.guga.walletserviceapi.service.WalletService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("${controller.path.base}/wallet-operator")
@Tag(name = "Wallet Operator", description = "Endpoints for wallet operations")
@SecurityRequirement(name = "bearerAuth")
public class WalletOperatorController {

    @Autowired
    WalletService walletService;

    @Autowired
    CustomerService customerService;

    @Autowired
    TransactionService transactionService;

    @Autowired
    MovementTransactionService movementTransactionService;

    @Autowired
    DepositSenderService depositSenderService;

    @Value("${spring.data.web.pageable.default-page-size}")
    private int defaultPageSize;
    
    @GetMapping("/{walletId}")
    public ResponseEntity<Wallet> getWalletById(@PathVariable Long walletId) {
        Wallet wallet = walletService.getWalletById(walletId);
        return ResponseEntity.ok(wallet);
    }

    // @GetMapping("/balance/{walletId}")
    // public ResponseEntity<Double> getWalletBalance(@PathVariable Long walletId) {
    //     Double balance = walletService.getWalletBalance(walletId);
    //     return ResponseEntity.ok(balance);
    // }

    @GetMapping("/last-movement/{walletId}")
    public ResponseEntity<Transaction> getLastTransactionByWalletId(@PathVariable Long walletId) {
        Transaction lastMovement = transactionService.getLastTransactionByWalletId(walletId);
        return ResponseEntity.ok(lastMovement);
    }

    @GetMapping("/transactions/search/{walletId}/period")
    public ResponseEntity<Page<Transaction>> searchTransactionsByPeriod(@PathVariable Long walletId, 
        @RequestParam(required = true) LocalDateTime startDate,
        @RequestParam(required = false) LocalDateTime endDate,
        @RequestParam(defaultValue = "0") int page) 
    {

        Pageable pageable = PageRequest.of(page, defaultPageSize,
                Sort.by(
                    Sort.Order.asc("createdAt"),
                    Sort.Order.asc("walletId")
                )
            );

        LocalDateTime endDateSend = startDate.plusDays(1);
        if (endDate != null) {
            endDateSend = endDateSend.plusDays(1);
        }

        Page<Transaction> transactions = transactionService
            .findByWalletIdAndCreatedAtBetween(walletId, startDate, endDateSend, pageable);
        return new ResponseEntity<>(transactions, HttpStatus.OK);

    }

    @PostMapping("/withdraw/")
    public ResponseEntity<Transaction> createWithdraw(
        @RequestBody CreateWithdrawRequest request) 
    {
        WithdrawMoney newWithdrawMoney = transactionService
            .saveWithdrawMoney(request.getWalletId(), request.getAmount());


        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(newWithdrawMoney.getTransactionId())
                .toUri();

        return ResponseEntity.created(location)
            .header("X-Trace-Id", ThreadContext.get("traceId"))
            .body(newWithdrawMoney);

    }

    @PostMapping("/deposit/{walletId}")
    public ResponseEntity<Transaction> createDeposit(@PathVariable Long walletId, 
        @RequestBody CreateDepositRequest request) 
    {

        DepositMoney newDepositMoney = transactionService.saveDepositMoney(request.getWalletId(), 
            request.getAmount(), request.getCpfSender(), request.getTerminalId(), request.getSenderName());


        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(newDepositMoney.getTransactionId())
                .toUri();

        return ResponseEntity.created(location)
            .header("X-Trace-Id", ThreadContext.get("traceId"))
            .body(newDepositMoney);
    }

    @PostMapping("/transfer/{walletId}")
    public ResponseEntity<Transaction> createTransfer(@PathVariable Long walletId, 
        @RequestBody CreateTransferRequest request) 
    {

        TransferMoneySend newTransferMoneySend = transactionService
            .saveTransferMoneySend(request.getWalletIdSend(), request.getWalletIdReceived(), request.getAmount());


        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(newTransferMoneySend.getTransactionId())
                .toUri();

        return ResponseEntity.created(location)
            .header("X-Trace-Id", ThreadContext.get("traceId"))
            .body(newTransferMoneySend);

    }

    @Operation(summary = "Upload the customer.json file.")
    @PostMapping("/upload-customer")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        content = @Content(
            mediaType = "multipart/form-data",
            schema = @Schema(implementation = FileUploadWrapper.class) 
        )
    )    
    public ResponseEntity<String> uploadCustomerJSON(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("O arquivo de upload está vazio.");
        }

        try {
            customerService.loadCsvAndSave(file);

            return ResponseEntity.ok("Carga de clientes via CSV concluída com sucesso.");

        } catch (Exception e) {
            // Captura a exceção e retorna um erro 500
            return ResponseEntity.internalServerError().body("Erro durante a carga do CSV: " + e.getMessage());
        }
    }

    @Operation(summary = "Upload the wallet.json file.")
    @PostMapping("/upload-wallet")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        content = @Content(
            mediaType = "multipart/form-data",
            schema = @Schema(implementation = FileUploadWrapper.class) 
        )
    )    
    public ResponseEntity<String> uploadWalletJSON(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("O arquivo de upload está vazio.");
        }

        try {
            walletService.loadCsvAndSave(file);

            return ResponseEntity.ok("Carga de clientes via CSV concluída com sucesso.");

        } catch (Exception e) {
            // Captura a exceção e retorna um erro 500
            return ResponseEntity.internalServerError().body("Erro durante a carga do CSV: " + e.getMessage());
        }
    }

    @Operation(summary = "Upload the transactions.json file.")
    @PostMapping("/upload-transactions")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
            mediaType = "multipart/form-data",
            schema = @Schema(implementation = FileUploadWrapper.class) 
        )
    )    
    public ResponseEntity<String> uploadTransactionCsv(
            @RequestParam("file") 
            MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("O arquivo de upload está vazio.");
        }

        try {
            transactionService.loadCsvAndSave(file);

            return ResponseEntity.ok("Carga de Transações via CSV concluída com sucesso.");

        } catch (Exception e) {
            // Captura a exceção e retorna um erro 500
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro durante a carga do CSV: " + e.getMessage());
        }
    }


    @Operation(summary = "Upload the moviments.json file.")
    @PostMapping("/upload-movements")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
            mediaType = "multipart/form-data",
            schema = @Schema(implementation = FileUploadWrapper.class) 
        )
    )    
    public ResponseEntity<String> uploadMovementsJSON(
            @RequestParam("file") 
            MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("O arquivo de upload está vazio.");
        }

        try {
            movementTransactionService.loadCsvAndSave(file);

            return ResponseEntity.ok("Carga de Transações via CSV concluída com sucesso.");

        } catch (Exception e) {
            // Captura a exceção e retorna um erro 500
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro durante a carga do CSV: " + e.getMessage());
        }
    }

    @Operation(summary = "Upload the deposit_sender.json file.")
    @PostMapping("/upload-deposit-senders")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
            mediaType = "multipart/form-data",
            schema = @Schema(implementation = FileUploadWrapper.class) 
        )
    )    
    public ResponseEntity<String> uploadDepositSenderJSON(
            @RequestParam("file") 
            MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("O arquivo de upload está vazio.");
        }

        try {
            depositSenderService.loadCsvAndSave(file);

            return ResponseEntity.ok("Carga de Transações via CSV concluída com sucesso.");

        } catch (Exception e) {
            // Captura a exceção e retorna um erro 500
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro durante a carga do CSV: " + e.getMessage());
        }
    }


}
