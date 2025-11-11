package com.guga.walletserviceapi.helpers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class FileUtils {

    private static final String FOLDER_OUT_FILE = "target/test-data";

    // Configura um ObjectMapper para formatar o JSON de forma legível (pretty print)
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT) // Habilita o "pretty print" (JSON formatado)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // Formata datas como ISO strings

    /**
     * Serializa uma lista de objetos para JSON e salva em um arquivo no diretório /target/test-data/.
     * @param fileName O nome do arquivo (ex: "customers.json")
     * @param data A lista de objetos a serem serializados
     */
    public static void writeListToJsonFile(String fileName, List<?> data) {
        try {
            // Cria o diretório de destino se não existir (geralmente em /target/test-data/)
            File directory = new File(FOLDER_OUT_FILE);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Define o caminho completo do arquivo
            File file = Paths.get(directory.getPath(), fileName).toFile();

            // Escreve a lista como JSON no arquivo
            objectMapper.writeValue(file, data);

            System.out.println("JSON salvo com sucesso em: " + file.getAbsolutePath());

        } catch (IOException e) {
            System.err.println("Erro ao salvar o arquivo JSON: " + fileName);
            e.printStackTrace();
            // Você pode lançar uma RuntimeException se quiser que o teste falhe
            // throw new RuntimeException("Falha ao salvar arquivo de teste.", e);
        }
    }

    /**
     * Escreve uma string de conteúdo em um arquivo no diretório /target/test-data/.
     * @param fileName O nome do arquivo (ex: "customers.json")
     * @param content A string (JSON) a ser escrita no arquivo.
     */
    public static void writeStringToFile(String fileName, String content) {
        try {
            // Cria o diretório de destino se não existir (geralmente em /target/test-data/)
            Path directoryPath = Paths.get(FOLDER_OUT_FILE);
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }

            // Define o caminho completo do arquivo
            Path filePath = directoryPath.resolve(fileName);

            // Escreve a string no arquivo usando UTF-8
            Files.writeString(filePath, content, StandardCharsets.UTF_8);

            System.out.println("Conteúdo salvo com sucesso em: " + filePath.toAbsolutePath());

        } catch (IOException e) {
            System.err.println("Erro ao salvar o arquivo: " + fileName);
            e.printStackTrace();
            // Pode relançar como RuntimeException se quiser que o teste falhe
            // throw new RuntimeException("Falha ao salvar arquivo de teste.", e);
        }
    }

    /**
     * Verifica se um arquivo existe e se foi criado há mais de 2 minutos.
     * @param fileName O caminho completo do arquivo.
     * @return true se o arquivo existir E sua data de criação for superior a 2 minutos atrás, false caso contrário.
     */
    public static boolean isOlderThanFiveMinutes(String fileName) {

        try {
            Path directoryPath = Paths.get(FOLDER_OUT_FILE);

            // Define o caminho completo do arquivo
            Path filePath = directoryPath.resolve(fileName);

            if (!Files.exists(filePath)) {
                return true;
            }

            // 2. Obtém os atributos básicos do arquivo
            BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
            //FileTime creationTimeFileTime = attrs.creationTime();
            FileTime lastModifiedTime = attrs.lastModifiedTime();

            // Converte FileTime (da API de arquivos) para Instant (API de tempo moderna)
            Instant lastModifiedInstant = lastModifiedTime.toInstant();

            // 3. Define o ponto de corte: 5 minutos atrás
            Instant fiveMinuteAgo = Instant.now().minus(5, ChronoUnit.MINUTES);

            // 4. Compara: A data de criação é anterior (mais antiga) que 2 minutos atrás?
            if (lastModifiedInstant.isBefore(fiveMinuteAgo)) {
                System.out.println("Arquivo criado em: " + lastModifiedInstant);
                System.out.println("Corte (5 min atrás): " + fiveMinuteAgo);
                System.out.println("Resultado: O arquivo é MAIS ANTIGO que 5 minutos.");
                return true;
            } else {
                System.out.println("Arquivo criado em: " + lastModifiedInstant);
                System.out.println("Corte (5 min atrás): " + fiveMinuteAgo);
                System.out.println("Resultado: O arquivo é MAIS NOVO ou tem 5 minutos.");
                return false;
            }

        } catch (IOException e) {
            System.err.println("Erro ao ler os atributos do arquivo: " + e.getMessage());
            return false;
        }
    }

    // Assumindo que você tem uma classe Transaction
    public static <T> List<T> loadListFromFile(String fileName,
                                               TypeReference<List<T>> typeRef) {
        ObjectMapper mapper = new ObjectMapper();

        // Garante que o ObjectMapper lide com LocalDate e LocalDateTime antes de carregar os JSONs
        if (!mapper.getRegisteredModuleIds().contains("jackson-datatype-jsr310")) {
            mapper.registerModule(new JavaTimeModule());
        }

        try {
            Path directoryPath = Paths.get(FOLDER_OUT_FILE);
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }

            // Define o caminho completo do arquivo
            Path filePath = directoryPath.resolve(fileName);

            return mapper.readValue(filePath.toFile(), typeRef);
        } catch (IOException e) {
            System.err.println("Erro ao carregar o arquivo: " + fileName);
            e.printStackTrace();
            return null;
        }
    }

}