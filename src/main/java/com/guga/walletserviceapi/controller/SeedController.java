package com.guga.walletserviceapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.guga.walletserviceapi.seeder.SeedRunner;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/seed")
@Tag(name = "Seed", description = "Endpoints for seeding data")
//@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class SeedController {

    private final SeedRunner seedRunner;

    @Operation(summary = "Run data seeder", description = "Executes the data seeding process on demand. Admin-only operation.")
    @PostMapping("/run")
    //@PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> runSeeder() {
        seedRunner.runSeed();
        return ResponseEntity.ok("Seeding process executed successfully.");
    }
}