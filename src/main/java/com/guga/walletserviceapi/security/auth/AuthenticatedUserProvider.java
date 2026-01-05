package com.guga.walletserviceapi.security.auth;


public interface AuthenticatedUserProvider {

    Long getCustomerId();
    Long getLoginId();
    String getLogin();
    Long getWalletId();
    boolean isAdmin();
    boolean isUser();
    boolean isSuport();
    boolean isSystem();
    boolean isAuthenticated();
    
}
