package com.guga.walletserviceapi.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.guga.walletserviceapi.audit.AuditLogContext;
import com.guga.walletserviceapi.audit.AuditLogger;
import com.guga.walletserviceapi.logging.LogMarkers;
import com.guga.walletserviceapi.seeder.SeedRunner;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RequestMapping("/seeder")
@RestController
@Tag(name = "Seed", description = "Endpoints for seeding data")
@RequiredArgsConstructor
public class SeedController {

    private static final Logger LOGGER = LogManager.getLogger(SeedController.class);

    private final SeedRunner seedRunner;

    @Operation(operationId = "seed_01_run", summary = "Run data seeder", description = "Executes the data seeding process on demand. Admin-only operation.")
    @PostMapping("/admin/run")
    public ResponseEntity<String> runSeeder() {
        AuditLogContext auditCtx = AuditLogContext.builder()
            .username("system")
            .build();

        LOGGER.info(LogMarkers.LOG, "SEED_RUN | trigger=manual");
        AuditLogger.log("SEED_RUN [START]", auditCtx);

        seedRunner.runSeed();

        AuditLogger.log(
            "SEED_RUN [SUCCESS]",
            auditCtx.toBuilder().info("result=completed").build()
        );

        return ResponseEntity.ok("Seeding process executed successfully.");
    }

}
