package com.guga.walletserviceapi.controller;

import java.net.URI;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.guga.walletserviceapi.dto.params.ParamAppCreateDTO;
import com.guga.walletserviceapi.dto.params.ParamAppResponseDTO;
import com.guga.walletserviceapi.dto.params.ParamAppUpdateDTO;
import com.guga.walletserviceapi.model.ParamApp;
import com.guga.walletserviceapi.service.ParamAppService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/params")
@Tag(name = "ParamApp", description = "Endpoints for managing application parameters")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class ParamAppController {

    private final ParamAppService paramAppService;

    // --- C: CREATE (POST) ---
    @PostMapping
    public ResponseEntity<ParamAppResponseDTO> createParam(@RequestBody @Valid ParamAppCreateDTO dto) {

        ParamApp paramAppInput = toEntity(dto);
        ParamApp savedParam = paramAppService.save(paramAppInput);
        
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedParam.getId())
                .toUri();

        return ResponseEntity.created(location).body(toDto(savedParam));
    }


    // --- R: READ ALL (GET) ---
    @GetMapping("/list")
    public ResponseEntity<List<ParamAppResponseDTO>> findAll() {

        Pageable pageable = PageRequest.of(0, 150,
                Sort.by(
                    Sort.Order.asc("name")
                )
            );

        List<ParamApp> params = paramAppService.findAll(pageable);
        List<ParamAppResponseDTO> response = params.stream().map(this::toDto).toList();
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    // --- R: READ BY ID (GET {id}) ---
    @GetMapping("/{id}")
    public ResponseEntity<ParamAppResponseDTO> findParamById(@PathVariable Long id) {

        ParamApp paramResult = paramAppService.findById(id);
        return new ResponseEntity<>(toDto(paramResult), HttpStatus.OK);

    }


    // --- U: UPDATE (PUT/PATCH) - Busca por ID ---
    @PutMapping("/update/{paramName}")
    public ResponseEntity<ParamAppResponseDTO> updateParam(
        @PathVariable String paramName, 
        @RequestBody @Valid ParamAppUpdateDTO dto) {

        ParamApp inputParamApp = toEntity(paramName, dto);
        ParamApp updatedParam = paramAppService.updateByName(paramName, inputParamApp);
        return new ResponseEntity<>(toDto(updatedParam), HttpStatus.OK);
        
    }


    @PutMapping("/adjust")
    public ResponseEntity<ParamAppResponseDTO> adjustSequenceParam(
        @RequestParam String paramName, 
        @RequestParam Long value) {

        ParamApp updatedParam = paramAppService.adjstSequenceId(paramName, value);
        return new ResponseEntity<>(toDto(updatedParam), HttpStatus.OK);
        
    }

    
    // --- D: DELETE (DELETE) ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Integer> deleteParam(@PathVariable Long id) {
        paramAppService.deleteById(id);
        return new ResponseEntity<>(0, HttpStatus.NO_CONTENT);
    }

    // =====================================================
    // MAPPERS (PRIVATE)
    // =====================================================

    private ParamAppResponseDTO toDto(ParamApp entity) {
        if (entity == null) return null;
        return new ParamAppResponseDTO(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getValueString(),
                entity.getValueLong(),
                entity.getValueInteger(),
                entity.getValueBigDecimal(),
                entity.getValueDate(),
                entity.getValueDateTime(),
                entity.isValueBoolean()
        );
    }

    private ParamApp toEntity(ParamAppCreateDTO dto) {
        return ParamApp.newParam(dto.name(), dto.description(), dto.description());
    }

    private ParamApp toEntity(String paramName, ParamAppUpdateDTO dto) {
        return ParamApp.newParam(paramName, dto.description(), dto.description());
    }
    
}