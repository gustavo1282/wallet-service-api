package com.guga.walletserviceapi.security.jwt;

import java.nio.charset.StandardCharsets; // 3. Adicionar esta importação
import java.security.Key;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.guga.walletserviceapi.model.LoginAuth;

import io.jsonwebtoken.Claims;
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


    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claimsResolver.apply(claims);
    }


    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(LoginAuth loginAuth) {
        Instant now = Instant.now()
                        .atZone(ZoneId.of("America/Sao_Paulo"))
                        .toInstant();
                        

        return Jwts.builder()            
            .subject(loginAuth.getLogin())
            .claim("loginId", loginAuth.getId())
            .claim("login", loginAuth.getLogin())
            .claim("customerId", loginAuth.getCustomerId())
            .claim("loginAuthType", loginAuth.getLoginAuthType())
            .claim("walletId", loginAuth.getWalletId())
            .claim("roles", loginAuth.getRole())
            .issuedAt( Date.from(now) )
            .expiration( Date.from(now.plusMillis(ACCESS_TOKEN_VALIDITY)) )
            .signWith(key, Jwts.SIG.HS256)
            .compact();
    }


    public String generateRefreshToken(LoginAuth loginAuth) {
        return Jwts.builder()
            .subject(loginAuth.getLogin())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_VALIDITY))
            .signWith(key, Jwts.SIG.HS256)
            .compact();
    }


    public String extractLogin(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractCustomerId(String token) {
        return extractClaim(token, claims -> claims.get("customerId", Long.class));
    }

    public Long extractLoginId(String token) {
        return extractClaim(token, claims -> claims.get("loginId", Long.class));
    }

    public String extractLoginType(String token) {
        return extractClaim(token, claims -> claims.get("loginAuthType", String.class));
    }

    public Long extractWalletId(String token) {
        return extractClaim(token, claims -> claims.get("walletId", Long.class));
    }

    public List<String> extractRoles(String token) {

        Claims claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();

        Object rolesClaim = claims.get("roles");

        if (rolesClaim instanceof List<?> rolesList) {
            return rolesList.stream()
                .map(String::valueOf)
                .distinct()
                .collect(Collectors.toList());
        }

        return List.of();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

}