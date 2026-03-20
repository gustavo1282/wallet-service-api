package com.guga.walletserviceapi.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.guga.walletserviceapi.model.LoginAuth;
import com.guga.walletserviceapi.model.enums.LoginRole;
import com.guga.walletserviceapi.repository.LoginAuthRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginAuthService implements UserDetailsService {

    private final LoginAuthRepository loginAuthRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    private Environment env;

    public LoginAuth register(String username, String password) {
        // 1. Busque o usuário no seu banco de dados
        LoginAuth loginAuth = loginAuthRepository.findByLogin(username)
            .orElseThrow(() -> new UsernameNotFoundException("Login não encontrado: " + username));

        // Criptografa a senha (AccessKey) antes de salvar
        String encodedPassword = passwordEncoder.encode(password);
        loginAuth.setAccessKey(encodedPassword);

        // Salva o usuário no banco de dados com a senha criptografada
        return loginAuthRepository.save(loginAuth);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Busque o usuário no seu banco de dados (lógica simplificada)
        LoginAuth loginAuth = findByLogin(username);

        // 2. Retorne um objeto UserDetails que o Spring Security entende
        return User.builder()
            .username(loginAuth.getLogin())
            .password(loginAuth.getAccessKey()) // Deve ser a senha JÁ criptografada do banco
            .roles(
                Optional.ofNullable(loginAuth.getRole())
                    .orElse(List.of())
                    .stream()
                    .map(LoginRole::name)
                    .toArray(String[]::new)
            )
            .build();
    }

    /**
     * Busca um usuário pelo seu login. Lógica simplificada.
     *
     * @param username O login do usuário.
     * @return O LoginAuth do usuário.
     * @throws UsernameNotFoundException se o usuário não for encontrado.
     */
    public LoginAuth findByLogin(String username) {
        return loginAuthRepository.findByLogin(username)
            .orElseThrow(() -> new UsernameNotFoundException("Login não encontrado: " + username));
    }

    /**
     * Retorna um registro de login aleatório do banco de dados.
     * Usado para ambientes de teste e desenvolvimento.
     *
     * @return Um LoginAuth aleatório ou lança exceção se nenhum for encontrado.
     */
    private LoginAuth findRandomLogin() {
        return Optional.ofNullable(loginAuthRepository.findAnyLogin().get())
            .filter(list -> !list.isEmpty())
            .map(list -> {
                var random = java.util.random.RandomGenerator.getDefault();
                int randomIndex = random.nextInt(list.size());
                return list.get(randomIndex);
            })
            .orElseThrow(() -> new UsernameNotFoundException("Nenhum usuário aleatório encontrado para o perfil de teste."));
    }
}