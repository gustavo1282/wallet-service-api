package com.guga.walletserviceapi.controller;

import com.guga.walletserviceapi.model.Wallet;
import com.guga.walletserviceapi.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

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
    public ResponseEntity<List<Wallet>> getAllWallets(
            Pageable pageable) {

        Page<Wallet> pageWallet = walletService.getAllWallets(pageable);

        if (pageWallet == null || pageWallet.isEmpty() || pageWallet.stream().toList().isEmpty()) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(pageWallet.stream().toList(), HttpStatus.OK);
    }

}
