package com.guga.walletserviceapi.model;

import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileUploadWrapper {
    
    // O compilador consegue processar esta anotação simples dentro de uma classe
    @Schema(type = "string", format = "binary", description = "Arquivo CSV para upload")
    private MultipartFile file;
}