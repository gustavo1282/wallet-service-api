package com.guga.walletserviceapi.security.auth;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import com.guga.walletserviceapi.logging.LogMarkers;
import com.guga.walletserviceapi.security.JwtAuthenticationDetails;

@Component
@RequestScope
public class JwtAuthenticatedUserProvider 
    implements AuthenticatedUserProvider 
{

    private static final Logger LOGGER = LogManager.getLogger(JwtAuthenticatedUserProvider.class);
    
    public JwtAuthenticationDetails get() {
        return getDetails();
    }
            
    @Override
    public Long getCustomerId() {
        return getDetails().getCustomerId();
    }

    @Override
    public Long getLoginId() {
        return getDetails().getLoginId();
    }

    @Override
    public Long getWalletId() {
        return getDetails().getWalletId();
    }

    @Override
    public boolean isAdmin() {
        return getDetails().getRoles().contains("ADMIN");
    }

    @Override
    public boolean isAuthenticated() {
        return SecurityContextHolder.getContext().getAuthentication() != null;
    }

    private JwtAuthenticationDetails getDetails() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof JwtAuthenticationDetails details)) {
            throw new IllegalStateException("JWT authentication details not found in principal");
        }

        LOGGER.info(LogMarkers.LOG, "AUTH={} AUTHORITIES={}", authentication, authentication.getAuthorities() );

        return details;
    }

    @Override
    public boolean isUser() {
        return getDetails().getRoles().contains("USER");
    }

    @Override
    public boolean isSuport() {
        return getDetails().getRoles().contains("SUPPORT");
    }

    @Override
    public boolean isSystem() {
        return getDetails().getRoles().contains("SYSTEM");
    }

    @Override
    public String getLogin() {
        return getDetails().getLogin();
    }
}
