package com.guga.walletserviceapi.controller;

import java.net.URI;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.guga.walletserviceapi.audit.AuditLogContext;
import com.guga.walletserviceapi.audit.AuditLogger;
import com.guga.walletserviceapi.dto.params.ParamAppCreateDTO;
import com.guga.walletserviceapi.dto.params.ParamAppMapper;
import com.guga.walletserviceapi.dto.params.ParamAppResponseDTO;
import com.guga.walletserviceapi.dto.params.ParamAppUpdateDTO;
import com.guga.walletserviceapi.logging.LogMarkers;
import com.guga.walletserviceapi.model.ParamApp;
import com.guga.walletserviceapi.security.auth.JwtAuthenticatedUserProvider;
import com.guga.walletserviceapi.service.ParamAppService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/params-app")
@Tag(name = "ParamApp", description = "Endpoints for managing application parameters")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN')")
@RequiredArgsConstructor
public class ParamAppController {

    private static final Logger LOGGER = LogManager.getLogger(ParamAppController.class);

    private final ParamAppService paramAppService;
    private final ParamAppMapper mapper;
    private final JwtAuthenticatedUserProvider authUserProvider;

    // --- R: READ ALL (GET) ---
    @Operation(
        operationId = "paramapp_01_listParams",
        summary = "List application parameters",
        description = "Returns a paginated list of application parameters ordered by name."
        )
    @GetMapping
    public ResponseEntity<List<ParamAppResponseDTO>> findAll() {
        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());

        LOGGER.info(LogMarkers.LOG, "PARAMAPP_LIST | admin={}", auditCtx.getUsername());

        Pageable pageable = PageRequest.of(0, 150,
                Sort.by(
                    Sort.Order.asc("name")
                )
            );

        List<ParamApp> params = paramAppService.findAll(pageable);
        List<ParamAppResponseDTO> response = params.stream().map(mapper::toDto).toList();

        AuditLogger.log(
            "PARAMAPP_LIST",
            auditCtx.toBuilder().info("rows=" + response.size()).build()
        );

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping
    @Operation(
        operationId = "paramapp_02_create",
        summary = "Create application parameter",
        description = "Creates a new application parameter and returns the created resource."
    )
    public ResponseEntity<ParamAppResponseDTO> createParam(@RequestBody @Valid ParamAppCreateDTO dto) {
        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());

        LOGGER.info(LogMarkers.LOG, "PARAMAPP_CREATE | admin={}", auditCtx.getUsername());
        AuditLogger.log("PARAMAPP_CREATE [START]", auditCtx);

        ParamApp paramAppInput = mapper.toEntity(dto);
        ParamApp savedParam = paramAppService.save(paramAppInput);
        
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedParam.getId())
                .toUri();

        AuditLogger.log(
            "PARAMAPP_CREATE [SUCCESS]",
            auditCtx.toBuilder().info("id=" + savedParam.getId()).build()
        );

        return ResponseEntity.created(location).body(mapper.toDto(savedParam));
    }

    // --- U: UPDATE (PUT/PATCH) - Busca por ID ---
    @Operation(
        operationId = "paramapp_03_update_by_name",
        summary = "Update application parameter by name",
        description = "Updates an existing application parameter identified by its name."
        )
    @PutMapping
    public ResponseEntity<ParamAppResponseDTO> updateParamByName(@PathVariable String paramName, 
        @RequestBody @Valid ParamAppUpdateDTO dto) {
        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());

        LOGGER.info(LogMarkers.LOG, "PARAMAPP_UPDATE_BY_NAME | paramName={} admin={}",
            paramName, auditCtx.getUsername()
        );
        AuditLogger.log(
            "PARAMAPP_UPDATE [START]",
            auditCtx.toBuilder().info("paramName=" + paramName).build()
        );

        ParamApp inputParamApp = mapper.toEntity(paramName, dto);
        ParamApp updatedParam = paramAppService.updateByName(paramName, inputParamApp);

        AuditLogger.log(
            "PARAMAPP_UPDATE [SUCCESS]",
            auditCtx.toBuilder().info("id=" + updatedParam.getId() + ",paramName=" + paramName).build()
        );

        return new ResponseEntity<>(mapper.toDto(updatedParam), HttpStatus.OK);
        
    }

    // --- R: READ BY ID (GET {id}) ---
    @Operation(
        operationId = "paramapp_04_get_by_id",
        summary = "Get application parameter by ID",
        description = "Returns a specific application parameter by its identifier."
        )
    @GetMapping("/{id}")
    public ResponseEntity<ParamAppResponseDTO> getById(@PathVariable Long id) {
        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());

        LOGGER.info(LogMarkers.LOG, "PARAMAPP_GET_BY_ID | id={} admin={}",
            id, auditCtx.getUsername()
        );

        ParamApp paramResult = paramAppService.findById(id);

        AuditLogger.log(
            "PARAMAPP_GET_BY_ID",
            auditCtx.toBuilder().info("id=" + id).build()
        );

        return new ResponseEntity<>(mapper.toDto(paramResult), HttpStatus.OK);

    }  
    
    // --- D: DELETE (DELETE) ---
    @DeleteMapping("/{id}")
    @Operation(
        operationId = "paramapp_05_delete_by_id",
        summary = "05 Delete application parameter by ID",
        description = "Deletes an application parameter identified by its ID."
    )
    public ResponseEntity<Integer> deleteParamById(@PathVariable Long id) {
        AuditLogContext auditCtx = AuditLogContext.from(authUserProvider.get());

        LOGGER.info(LogMarkers.LOG, "PARAMAPP_DELETE_BY_ID | id={} admin={}",
            id, auditCtx.getUsername()
        );
        AuditLogger.log(
            "PARAMAPP_DELETE [START]",
            auditCtx.toBuilder().info("id=" + id).build()
        );

        // paramAppService.deleteById(id);

        AuditLogger.log(
            "PARAMAPP_DELETE [SUCCESS]",
            auditCtx.toBuilder().info("id=" + id).build()
        );

        return new ResponseEntity<>(0, HttpStatus.NO_CONTENT);
    }

}
