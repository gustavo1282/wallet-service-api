package com.guga.walletserviceapi.helpers;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

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

    @Value("${WALLET_USER}")
    public static final String APP_WALLET_USER = "wallet_user";

    @Value("${WALLET_PASS}")
    public static final String APP_WALLET_PASS = "wallet_pass";

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

    /***
     * Remove tudo que não for número de uma string, útil para limpar CPF, CNPJ, Telefone, etc
     */
    public static String onlyNumbers(String texto) {
        return texto.replaceAll("\\D", "");
    }

    public static String blankToNull(String value) {
        if (value == null || value.trim().isEmpty() || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public static String generateKeyToLoginAuth(Long walletId) {
        return GlobalHelper.APP_WALLET_PASS + "_" + walletId + "_@";
    }

    public static String normalizeString(String text) {
        if (text == null) return null;

        // 1. Remove acentos (ex: 'á' vira 'a' + '´')
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        
        // 2. Remove os sinais diacríticos (os acentos separados)
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String withoutAccents = pattern.matcher(normalized).replaceAll("");

        // 3. Mantém apenas letras (a-z, A-Z) e números (0-9)
        // O ^ dentro do colchete significa "NÃO", ou seja, substitui tudo que NÃO for alfanumérico por ""
        return withoutAccents.replaceAll("[^a-zA-Z0-9 ]", "");
    }

}
