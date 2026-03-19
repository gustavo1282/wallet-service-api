package com.guga.walletserviceapi.dto.params;

import com.guga.walletserviceapi.model.enums.Status;

public record ParamAppUpdateDTO(
    String value,
    String description,
    Status status
)
{ }