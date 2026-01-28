package com.guga.walletserviceapi.helpers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.guga.walletserviceapi.logging.LogMarkers;

public class GlobalHelper {

    public static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

    public static final String PATTERN_FORMAT_DATE = "yyyy-MM-dd";
    public static final String PATTERN_FORMAT_DATE_TIME = "yyyy-MM-dd HH:mm:ss.SSSSSSSSS";

    public static final String APP_USER_NAME = "wallet_user";
    public static final String APP_PASSWORD = "wallet_pass";

    @Value("${spring.jpa.properties.hibernate.jdbc.batch_size}")
    public static int BATCH_SIZE;

    public static Pageable getDefaultPageable() {
        return PageRequest.of(0, 150,
                Sort.by(
                    Sort.Order.desc("createdAt")
                )
            );
    }

    public static Map<String, String> jwtTokens(String token) {
        String accessToken = "mock-access-token." + UUID.randomUUID().toString();
        String refreshToken = "mock-refresh-token." + UUID.randomUUID().toString();

        Map<String, String> tokens = new HashMap<>();
        tokens.put("mock-access-token", accessToken);
        tokens.put("mock-refresh-token", refreshToken);

        return tokens;
    }

    public static long convertLocalDateTimeToMillis() {
        
        LocalDateTime localDateTime = LocalDateTime.now();

        ZoneId zoneId = ZoneId.of("America/Sao_Paulo");

        ZonedDateTime zonedDateTime = localDateTime.atZone(zoneId);

        Instant instant = zonedDateTime.toInstant();

        return instant.toEpochMilli();
    }

    public static void printAllVariables(Environment env, Logger logger) {

        if (env instanceof org.springframework.core.env.AbstractEnvironment abstractEnv) {

            abstractEnv.getPropertySources().forEach(source -> {
                logger.info(LogMarkers.LOG, ">>> PropertySource: " + source.getName());

                if (source instanceof org.springframework.core.env.EnumerablePropertySource<?> eps) {
                    for (String name : eps.getPropertyNames()) {
                        Object value = eps.getProperty(name);
                        logger.info(LogMarkers.LOG, "  {} = {}", name, value);
                    }
                } else {
                    logger.info(LogMarkers.LOG, "  (non-enumerable)");
                }

                logger.info(LogMarkers.LOG, " ".repeat(5));
                }            
            );
        }
    }

}
