package com.guga.walletserviceapi.security;

import java.nio.charset.StandardCharsets; // 3. Adicionar esta importação
import java.security.Key;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys; // 1. Adicionar esta importação
import jakarta.annotation.PostConstruct; 

@Service
public class JwtService {

    private SecretKey key;

    private static final long ACCESS_TOKEN_VALIDITY = 45 * 60 * 1000;             // 45 minutos
    private static final long REFRESH_TOKEN_VALIDITY = 2 * 24 * 60 * 60 * 1000;   // 2 dias

    @Value("${jwt.secret}")
    private String jwtSecret;

    // 4. Descomentar e usar este método para inicializar a chave APÓS a injeção do @Value
    @PostConstruct
    public void init() {
        if (jwtSecret == null || jwtSecret.isEmpty()) {
             // Isso garante um erro claro se a variável não for encontrada no Vault ou no ambiente
            throw new IllegalArgumentException("JWT_SECRET property is not set or is empty!");
        }
        // A chave precisa ter pelo menos 256 bits (32 bytes)
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /* REMOVER OS COMENTÁRIOS DESTE MÉTODO - NÃO É MAIS NECESSÁRIO */
    /*private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }*/

    public String generateAccessToken(String username) {
        String roles = ""; 
        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_VALIDITY))
                .signWith(key, Jwts.SIG.HS256) 
                .compact();

    }

    public String generateRefreshToken(String username) {
        String roles = ""; 
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_VALIDITY))
                .signWith(key, Jwts.SIG.HS256) // A variável 'key' agora está inicializada
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(key) // A variável 'key' agora está inicializada
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(key) // A variável 'key' agora está inicializada
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
  
  }

}