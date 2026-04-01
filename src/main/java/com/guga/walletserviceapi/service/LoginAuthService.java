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
    private LoginAuth findRandomLogin() {
        LOGGER.info("LOGIN_AUTH_SERVICE_FIND_RANDOM_ENTRY");

        LoginAuth randomLogin = Optional.ofNullable(loginAuthRepository.findAnyLogin().get())
            .filter(list -> !list.isEmpty())
            .map(list -> {
                var random = java.util.random.RandomGenerator.getDefault();
                int randomIndex = random.nextInt(list.size());
                return list.get(randomIndex);
            })
            .orElseThrow(() -> new UsernameNotFoundException("Nenhum usuario aleatorio encontrado para o perfil de teste."));

        LOGGER.info("LOGIN_AUTH_SERVICE_FIND_RANDOM_SUCCESS | loginId={} username={}", randomLogin.getId(), randomLogin.getLogin());
        return randomLogin;
    }
}
