package com.guga.walletserviceapi.controller;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.guga.walletserviceapi.model.Wallet;
import com.guga.walletserviceapi.service.WalletService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("${controller.path.base}/wallets")
@Tag(name = "Wallet", description = "Endpoints for managing wallets")
public class WalletController {

    @Autowired
    private WalletService walletService;

    @Operation(summary = "Create a new Wallet", description = "Creates a new Wallet with the data provided in the request body.")
    @PostMapping("/wallet")
    public ResponseEntity<Wallet> createWallet(@RequestBody Wallet wallet) {

        Wallet createdWallet = walletService.saveWallet(wallet);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdWallet.getWalletId())
                .toUri();

        return ResponseEntity.created(location).body(createdWallet);
    }

    @Operation(summary = "Get Wallet by ID", description = "Retrieves a Wallet by their ID provided in the request body.")
    @GetMapping("/{id}")
    public ResponseEntity<Wallet> getWalletById(@PathVariable Long id) {
        Wallet wallet = walletService.getWalletById(id);
        return new ResponseEntity<>(wallet, HttpStatus.OK);
    }
    
    @Operation(summary = "Update user by ID", description = "Updates a user by their ID provided in the request body.")
    @PutMapping("/{id}")
    public ResponseEntity<Wallet> updateWallet(
            @PathVariable Long id,
            @RequestBody @Valid Wallet walletUpdate) {

        Wallet wallet = walletService.updateWallet(id, walletUpdate);
        return new ResponseEntity<>(wallet, HttpStatus.OK);
    }

    @Operation(summary = "Get all Wallets", description = "Retrieves all Wallets.")
    @GetMapping("/list")
    public ResponseEntity<Page<Wallet>> getAllWallets() {

        Pageable pageable = PageRequest.of(0, 20,
                Sort.by(
                    Sort.Order.asc("walletId"),
                    Sort.Order.asc("createdAt")                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         
                )
            );

        Page<Wallet> pageWallet = walletService.getAllWallets(pageable);

        return new ResponseEntity<>(pageWallet, HttpStatus.OK);

    }

    @Operation(summary = "Get Wallets by Customer ID",
            description = "Retrieves a list Wallets by Customer ID provided in the request body.")
    @GetMapping(value = "/search-by-customer")
    public ResponseEntity<Page<Wallet>> getWalletByCustomerId(
            @RequestParam(required = true) Long customerId) {

        Pageable pageable = PageRequest.of(0, 20,
                Sort.by(
                    Sort.Order.asc("customerId"),
                    Sort.Order.asc("walletId")                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         
                )
            );
        
        Page<Wallet> findResult = walletService.getWalletByCustomerId(customerId, pageable);

        return new ResponseEntity<>(findResult, HttpStatus.OK);
    }


}
