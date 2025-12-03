package com.guga.walletserviceapi.controller;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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

import com.guga.walletserviceapi.model.ParamApp;
import com.guga.walletserviceapi.service.ParamAppService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("${controller.path.base}/params")
@Tag(name = "ParamApp", description = "Endpoints for managing ParamApp")
@RequiredArgsConstructor
public class ParamAppController {

    @Autowired
    private ParamAppService paramAppService;

    // --- C: CREATE (POST) ---
    @PostMapping
    public ResponseEntity<ParamApp> createParam(@RequestBody ParamApp paramAppInput) {

        ParamApp savedParam = paramAppService.save(paramAppInput);
        
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedParam.getId())
                .toUri();

        return ResponseEntity.created(location).body(savedParam);
    }

    // --- R: READ ALL (GET) ---
    @GetMapping("/list")
    public ResponseEntity<List<ParamApp>> findAll() {

        List<ParamApp> params = paramAppService.findAll();
        
        return new ResponseEntity<>(params, HttpStatus.OK);
    }

    // --- R: READ BY ID (GET {id}) ---
    @GetMapping("/{id}")
    public ResponseEntity<ParamApp> findParamById(@PathVariable Long id) {

        ParamApp paramResult = paramAppService.findById(id);
        return new ResponseEntity<>(paramResult, HttpStatus.OK);

    }

    // --- U: UPDATE (PUT/PATCH) - Busca por ID ---
    @PutMapping("/update")
    public ResponseEntity<ParamApp> updateParam(
        @PathVariable String paramName, 
        @RequestBody ParamApp inputParamApp) {

        ParamApp updatedParam = paramAppService.updateByName(paramName, inputParamApp);
        return new ResponseEntity<>(updatedParam, HttpStatus.OK);
        
    }

    @PutMapping("/adjust")
    public ResponseEntity<ParamApp> adjustSequenceParam(
        @RequestParam String paramName, 
        @RequestParam Long value) {

        ParamApp updatedParam = paramAppService.adjstSequenceId(paramName, value);
        return new ResponseEntity<>(updatedParam, HttpStatus.OK);
        
    }

    
    // --- D: DELETE (DELETE) ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Integer> deleteParam(@PathVariable Long id) {
        paramAppService.deleteById(id);
        return new ResponseEntity<>(0, HttpStatus.NO_CONTENT);
    }
    
}