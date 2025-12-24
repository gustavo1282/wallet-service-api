package com.guga.walletserviceapi.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.guga.walletserviceapi.model.FileUploadWrapper;
import com.guga.walletserviceapi.model.Transaction;
import com.guga.walletserviceapi.service.CustomerService;
import com.guga.walletserviceapi.service.DepositSenderService;
import com.guga.walletserviceapi.service.MovementTransactionService;
import com.guga.walletserviceapi.service.TransactionService;
import com.guga.walletserviceapi.service.WalletService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

    /*
    @Operation(summary = "Get Wallet by ID", description = "Retrieves a Wallet by their ID provided in the request body.")
    @GetMapping("/wallet/{id}")
    public ResponseEntity<Wallet> getWalletById(
            @Parameter(description = "ID of the Wallet", required = true)
            @PathVariable(name = "walletId", required = true)
            Long id) {

        Wallet wallet = walletService.getWalletById(id);
        return new ResponseEntity<>(wallet, HttpStatus.OK);

    }
    */

    @GetMapping("/transaction/{transactionId}")
    @Operation(summary = "Get Transaction by ID", description = "Retrieves a Transaction by their ID provided in the request body.")
    public ResponseEntity<Transaction> getTransactionById(
            @Parameter(description = "ID of the Transaction", required = true)
            @PathVariable(name = "transactionId", required = true)
            Long id) {

        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    @GetMapping("/transactions/{walletId}/{date}")
    @Operation(summary = "Get all Transactions", description = "Retrieves all Transactions.")
    public ResponseEntity<List<Transaction>> getTransactionFilter(
            @Parameter(description = "ID of the wallet", required = true)
            @PathVariable(name = "walletId", required = true)
            Long walletId,

            @Parameter(description = "Date of the transaction", required = true)
            @PathVariable(name = "date", required = true)
            LocalDate date
    ) {

        List<Transaction> transactions = new ArrayList<Transaction>() ;
//        for (int i = 0; i < 10; i++) {
//            transactions.add(Transaction.newSampleTransaction());
//        }
        return new ResponseEntity<>(transactions, HttpStatus.OK);
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
