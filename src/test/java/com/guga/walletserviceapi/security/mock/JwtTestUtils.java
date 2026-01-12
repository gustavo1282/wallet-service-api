package com.guga.walletserviceapi.security.mock;

import java.util.Map;

public class JwtTestUtils {

    public static String fakeToken() {
        return "Bearer fake.jwt.token";
    }

    public static Map<String, Object> defaultClaims() {
        return Map.of(
            "username", "user@test.com",
            "customer_id", 1L,
            "wallet_id", 10L,
            "roles", "ROLE_USER"
        );
    }
}
