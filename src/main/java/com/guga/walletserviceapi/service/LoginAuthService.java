package com.guga.walletserviceapi.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.guga.walletserviceapi.helpers.GlobalHelper;
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
    private org.springframework.core.env.Environment env;

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
        // 1. Busque o usuário no seu banco de dados
        LoginAuth loginAuth = loginAuthRepository.findByLogin(username)
            .orElseThrow(() -> new UsernameNotFoundException("Login não encontrado: " + username));

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

    public LoginAuth findByLogin(String username) {
        
        LoginAuth loginAuth = null;

        if (GlobalHelper.blankToNull(env.getProperty("WALLET_USER")).equals(username)){
            if (GlobalHelper.blankToNull(env.getProperty("PROFILE")).equals("dev") || 
                GlobalHelper.blankToNull(env.getProperty("PROFILE")).equals("local")){
                loginAuth = anyLogin();
            }
        }

        if (loginAuth == null || loginAuth.getLogin() == null) {
            Optional<LoginAuth> loginAuthOptional= loginAuthRepository.findByLogin(username);
            loginAuth = (loginAuthOptional.isPresent()) ? loginAuthOptional.get(): null;
        }

        return loginAuth;
    }

    public LoginAuth anyLogin() {

        List<LoginAuth> result = loginAuthRepository.findAnyLogin().get();
        
        // getFirst() é um método novo da SequencedCollection no Java 21
        return result.isEmpty() ? null : result.getFirst();

        /*** 
            return Optional.ofNullable(loginAuthRepository.findAnyLogin().get())
            .filter(list -> !list.isEmpty()) // Evita erro se a query não trouxer nada
            .map(list -> {
                // No Java 21, RandomGenerator é a forma moderna e eficiente
                var random = java.util.random.RandomGenerator.getDefault();
                int randomIndex = random.nextInt(list.size());
                
                return list.get(randomIndex); // Retorna o registro sorteado (ex: o 15º)
            })
            .orElse(null); // Retorna null se a lista for nula ou vazia
        ***/
    }
    
}