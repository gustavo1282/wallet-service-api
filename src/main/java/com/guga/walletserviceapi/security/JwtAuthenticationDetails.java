package com.guga.walletserviceapi.security;

import java.io.Serializable;
import java.util.List;

import com.guga.walletserviceapi.model.enums.LoginRole;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class JwtAuthenticationDetails implements Serializable {

    private final Long loginId;
    private final String login;
    private final Long customerId;
    private final Long walletId;
    private final String loginType;
    private final List<LoginRole> roles;
    
}