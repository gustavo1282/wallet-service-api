package com.guga.walletserviceapi.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
public class LoginAutenticateHeader {

    public LoginAutenticateHeader(String loginName, String application, Boolean autenticated, String loginSession) {
        this.loginName = loginName;
        this.application = application;
        this.autenticated = autenticated;
        this.loginSession = loginSession;
    }

    private String loginName;

    private String application;

    private Boolean autenticated;

    private String loginSession;

}
