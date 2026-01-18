package com.guga.walletserviceapi.helpers;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import net.datafaker.Faker;

/**
 * Classe utilitária para geração de strings e sequências aleatórias.
 */
public class RandomMock {

    // Conjunto de todos os caracteres alfabéticos (maiúsculos e minúsculos)
    private static final String ALPHABETIC_CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    // Conjunto de todos os dígitos numéricos
    private static final String NUMERIC_CHARS = "0123456789";

    // Recomenda-se usar SecureRandom para maior imprevisibilidade,
    // mas Random é suficiente para a maioria dos casos de uso não criptográfico.
    private static final Random RANDOM = new SecureRandom();

    /**
     * Gera uma string aleatória contendo apenas letras (maiúsculas e minúsculas).
     *
     * @param length O número de caracteres desejado para a string.
     * @return Uma string de letras aleatórias.
     * @throws IllegalArgumentException se o tamanho for menor ou igual a zero.
     */
    public static String generateRandomLetters(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("O tamanho deve ser maior que zero.");
        }

        StringBuilder sb = new StringBuilder(length);
        int maxIndex = ALPHABETIC_CHARS.length();

        for (int i = 0; i < length; i++) {
            // Seleciona um índice aleatório no conjunto de caracteres
            int randomIndex = RANDOM.nextInt(maxIndex);

            // Adiciona o caractere correspondente ao StringBuilder
            sb.append(ALPHABETIC_CHARS.charAt(randomIndex));
        }

        return sb.toString();
    }

    /**
     * Gera uma string aleatória contendo apenas dígitos numéricos.
     *
     * @param length O número de dígitos desejado para a string.
     * @return Uma string de dígitos aleatórios.
     * @throws IllegalArgumentException se o tamanho for menor ou igual a zero.
     */
    public static String generateRandomNumbers(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("O tamanho deve ser maior que zero.");
        }

        StringBuilder sb = new StringBuilder(length);
        int maxIndex = NUMERIC_CHARS.length();

        for (int i = 0; i < length; i++) {
            // Seleciona um índice aleatório no conjunto de dígitos (0-9)
            int randomIndex = RANDOM.nextInt(maxIndex);

            // Adiciona o caractere correspondente ao StringBuilder
            sb.append(NUMERIC_CHARS.charAt(randomIndex));
        }

        return sb.toString();
    }

    public static LocalDate getDateNowMinus(int year, int month, int day) {
        LocalDate dateNow = LocalDate.now();
        dateNow = dateNow.minusYears(year);
        dateNow = dateNow.minusMonths(month);
        dateNow = dateNow.minusDays(day);
        return dateNow;
    }

    public static int generateIntNumber(int value) {
        return ThreadLocalRandom.current().nextInt(value);
    }

    public static int generateIntNumberByInterval(int init, int end) {
        return ThreadLocalRandom.current().nextInt(init, end);
    }

    public static LocalDateTime generatePastLocalDateTime(int yearsToSubtract){
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twoYearsAgo = now.minusYears(yearsToSubtract);

        // Converte as datas para segundos desde a época para gerar o valor aleatório
        long startEpochSeconds = twoYearsAgo.atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
        long endEpochSeconds = now.atZone(java.time.ZoneId.systemDefault()).toEpochSecond();

        // Gera um valor aleatório de segundos dentro do intervalo
        long randomEpochSeconds = ThreadLocalRandom.current().nextLong(startEpochSeconds, endEpochSeconds);

        // Converte o valor aleatório de segundos de volta para LocalDateTime
        return LocalDateTime.ofEpochSecond(randomEpochSeconds, 0, java.time.ZoneOffset.systemDefault().getRules().getOffset(now));
    }

    public static String removeSufixoEPrevixos(String text) {
        // Lista de prefixos e sufixos comuns em pt_BR para remover
        String[] PREFIXOS = {"Dr.", "Dra.", "Sr.", "Sra.", "Srta.", "Me.", "Ma.", "Eng.", "Eng.ª", "Prof.", "Profa.", "Pr.", "Pra.", "Arq", "Arqa.", "Rev."};
        String[] SUFIXOS = {"Jr.", "Filho", "Neto", "Sobrinho"};

        for (String prefix : PREFIXOS) {
            if (text.toUpperCase().startsWith(prefix.toUpperCase())) {
                text = text.substring(prefix.length()).trim();
                break;
            }
        }
        for (String suffix : SUFIXOS) {
            if (text.toUpperCase().endsWith(suffix.toUpperCase())) {
                text = text.substring(0, text.length() - suffix.length()).trim();
                break;
            }
        }
        return text;
    }

    public static String generateHexaBetween100And999() {
        return Integer.toHexString(generateIntNumberByInterval(100, 999) );
    }

    public static String cpfFake() {
        Faker faker = new Faker(new Locale("pt-BR"));
        return faker.cpf().valid();
    }

    public static String loginFakeMock() {
        int idx = ThreadLocalRandom.current().nextInt(3);
        String[] logins = new String[]{"uKEIX0992", "uMAAEQ5210", "uBEWP5021"};
        try {
            return logins[idx];
        } catch (Exception e) {
            return null;
        }
    }

    public static int generateNumberByIntervalAndException(int min, int max, List<Integer> exceptions) {
        // 1. Criar a lista de números válidos
        List<Integer> validNumbers = new ArrayList<>();
        for (int i = min; i <= max; i++) {
            if (!exceptions.contains(i)) {
                validNumbers.add(i);
            }
        }

        // Verificar se restou algum número válido após as exclusões
        if (validNumbers.isEmpty()) {
            return -1;
        }

        // 2. Gerar um índice aleatório
        Random random = new Random();
        // O índice deve ser entre 0 (inclusivo) e o tamanho da lista (exclusivo)
        int randomIndex = random.nextInt(validNumbers.size()); 

        // 3. Retornar o número na posição aleatória
        return validNumbers.get(randomIndex);

    }

    public static String generatePhoneNumberAleatory() {
        // Gera um número de telefone no formato (XX) 9XXXX-XXXX
        String ddd = String.format("%02d", generateIntNumberByInterval(11, 99)); // DDD entre 11 e 99
        String firstPart = "9" + String.format("%04d", generateIntNumberByInterval(0, 9999)); // Primeiro bloco com 5 dígitos
        String secondPart = String.format("%04d", generateIntNumberByInterval(0, 9999)); // Segundo bloco com 4 dígitos

        return String.format("(%s) %s-%s", ddd, firstPart, secondPart);
    }

}