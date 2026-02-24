package com.guga.walletserviceapi.dto.params;

import jakarta.validation.constraints.NotBlank;

public record ParamAppCreateDTO(
    @NotBlank String name,
    @NotBlank String value,
    String description
) 
{ }