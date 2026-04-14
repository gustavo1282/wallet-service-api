package com.guga.walletserviceapi.service;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.guga.walletserviceapi.exception.ResourceBadRequestException;
import com.guga.walletserviceapi.logging.LogMarkers;
import com.guga.walletserviceapi.model.LoginAuth;
import com.guga.walletserviceapi.model.enums.LoginRole;
import com.guga.walletserviceapi.repository.LoginAuthRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginAuthService implements UserDetailsService {

    private static final Logger LOGGER = LogManager.getLogger(LoginAuthService.class);

    private final LoginAuthRepository loginAuthRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(rollbackFor = Exception.class)
    public LoginAuth register(String username, String password) {
        LOGGER.info("LOGIN_AUTH_SERVICE_REGISTER_ENTRY | username={}", username);

        LoginAuth loginAuth = loginAuthRepository.findByLogin(username)
            .orElseThrow(() -> new UsernameNotFoundException("Login nao encontrado: " + username));

        String encodedPassword = passwordEncoder.encode(password);
        loginAuth.setAccessKey(encodedPassword);

        LoginAuth saved = loginAuthRepository.save(loginAuth);
        LOGGER.info("LOGIN_AUTH_SERVICE_REGISTER_SUCCESS | loginId={} username={}", saved.getId(), saved.getLogin());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        LOGGER.info("LOGIN_AUTH_SERVICE_LOAD_USER_ENTRY | username={}", username);

        LoginAuth loginAuth = findByLogin(username);

        UserDetails details = User.builder()
            .username(loginAuth.getLogin())
            .password(loginAuth.getAccessKey())
            .roles(
                Optional.ofNullable(loginAuth.getRole())
                    .orElse(List.of())
                    .stream()
                    .map(LoginRole::name)
                    .toArray(String[]::new)
            )
            .build();

        LOGGER.info("LOGIN_AUTH_SERVICE_LOAD_USER_SUCCESS | username={} rolesCount={}",
            username,
            Optional.ofNullable(loginAuth.getRole()).orElse(List.of()).size()
        );
        return details;
    }

    @Transactional(readOnly = true)
    public LoginAuth findByLogin(String username) {
        LOGGER.info("LOGIN_AUTH_SERVICE_FIND_BY_LOGIN_ENTRY | username={}", username);

        LoginAuth found = loginAuthRepository.findByLogin(username)
            .orElseThrow(() -> new UsernameNotFoundException("Login nao encontrado: " + username));

        LOGGER.info("LOGIN_AUTH_SERVICE_FIND_BY_LOGIN_SUCCESS | loginId={} username={}", found.getId(), found.getLogin());
        return found;
    }

    @Transactional(readOnly = true)
    public LoginAuth findAnyLoginWithTransactions(String username, String password) {
        LOGGER.info("LOGIN_AUTH_SERVICE_FIND_ANY_LOGIN_WITH_TRN");

        boolean isAnyUser = (username.equals("anyuser") &&
                             password.equals("anypassword"));

        if (!isAnyUser) {
            LOGGER.warn(LogMarkers.LOG, "AUTH_ANY_LOGIN_UNAUTHORIZED | requestedUser={}", username);
            throw new ResourceBadRequestException("LOGIN_UNAUTHORIZED");
        }

        Optional<LoginAuth> dataRet = loginAuthRepository.findAnyLoginWithTransactions();
        if (dataRet == null || dataRet.isEmpty()) {
            LOGGER.info("LOGIN_AUTH_SERVICE_FIND_ANY_LOGIN_WITH_TRN");
            throw new UsernameNotFoundException("Nenhum login disponível.");
        }

        LoginAuth loginAuth = dataRet.get();

        LOGGER.info("LOGIN_AUTH_SERVICE_FIND_ANY_LOGIN_WITH_TRN_SUCCESS | loginId={} username={}", 
            loginAuth.getId(), 
            loginAuth.getLogin()
        );

        return loginAuth;
    }

}
