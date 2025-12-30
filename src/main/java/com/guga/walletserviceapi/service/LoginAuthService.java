package com.guga.walletserviceapi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.guga.walletserviceapi.model.LoginAuth;
import com.guga.walletserviceapi.repository.LoginAuthRepository;
import com.guga.walletserviceapi.security.JwtService;

@Service
public class LoginAuthService implements UserDetailsService {

    @Autowired
    private LoginAuthRepository loginAuthRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;


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

    public String login(LoginAuth loginAuth) {
        return jwtService.generateAccessToken(loginAuth.getLogin());
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Busque o usuário no seu banco de dados
        LoginAuth loginAuth = loginAuthRepository.findByLogin(username)
            .orElseThrow(() -> new UsernameNotFoundException("Login não encontrado: " + username));

        // 2. Retorne um objeto UserDetails que o Spring Security entende
        return User.builder()
            .username(loginAuth.getLogin())
            .password(loginAuth.getAccessKey()) // Deve ser a senha JÁ criptografada do banco
            .roles("USER") // Adicione os papéis/autoridades aqui
            .build();
    }
}