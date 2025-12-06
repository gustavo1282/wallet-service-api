package com.guga.walletserviceapi.helpers;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class GlobalHelper {

    public static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

    public static final String PATTERN_FORMAT_DATE = "yyyy-MM-dd";
    public static final String PATTERN_FORMAT_DATE_TIME = "yyyy-MM-dd HH:mm:ss.SSSSSSSSS";

    @Value("${spring.jpa.properties.jdbc.batch_size}")
    public static int BATCH_SIZE;

    public static Pageable getDefaultPageable() {
        return PageRequest.of(0, 50,
                Sort.by(
                    Sort.Order.asc("createdAt")
                )
            );
    }

    public static List<String> matchers() {
        List<String> matchers = Arrays.asList(
            "/actuator/**",
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/h2-console/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/wallet-service-api/**",
            "/wallet-service-api/api/**"
            );
        return matchers;
    }

}
