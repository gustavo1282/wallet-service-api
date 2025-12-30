package com.guga.walletserviceapi.model.dto;

import java.util.List;

import com.guga.walletserviceapi.model.enums.LoginAuthType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthLoginDataResponse {
    private String login;
    private LoginAuthType loginAuthType;
    private Long customerId;
    private List<Long> walletIds;
}
