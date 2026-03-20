package com.guga.walletserviceapi.controller;

import java.net.URI;
import java.util.List;

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

import com.guga.walletserviceapi.dto.params.ParamAppCreateDTO;
import com.guga.walletserviceapi.dto.params.ParamAppMapper;
import com.guga.walletserviceapi.dto.params.ParamAppResponseDTO;
import com.guga.walletserviceapi.dto.params.ParamAppUpdateDTO;
import com.guga.walletserviceapi.model.ParamApp;
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

    private final ParamAppService paramAppService;
    private final ParamAppMapper mapper;

    // --- R: READ ALL (GET) ---
    @Operation(
        operationId = "paramapp_01_listParams",
        summary = "List application parameters",
        description = "Returns a paginated list of application parameters ordered by name."
        )
    @GetMapping
    public ResponseEntity<List<ParamAppResponseDTO>> findAll() {

        Pageable pageable = PageRequest.of(0, 150,
                Sort.by(
                    Sort.Order.asc("name")
                )
            );

        List<ParamApp> params = paramAppService.findAll(pageable);
        List<ParamAppResponseDTO> response = params.stream().map(mapper::toDto).toList();
        
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping
    @Operation(
        operationId = "paramapp_02_create",
        summary = "Create application parameter",
        description = "Creates a new application parameter and returns the created resource."
    )
    public ResponseEntity<ParamAppResponseDTO> createParam(@RequestBody @Valid ParamAppCreateDTO dto) {

        ParamApp paramAppInput = mapper.toEntity(dto);
        ParamApp savedParam = paramAppService.save(paramAppInput);
        
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedParam.getId())
                .toUri();

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

        ParamApp inputParamApp = mapper.toEntity(paramName, dto);
        ParamApp updatedParam = paramAppService.updateByName(paramName, inputParamApp);
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

        ParamApp paramResult = paramAppService.findById(id);
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
        //paramAppService.deleteById(id);
        return new ResponseEntity<>(0, HttpStatus.NO_CONTENT);
    }

}