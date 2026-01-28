package com.guga.walletserviceapi.helpers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class FileUtils {

    public static final String SEED_FOLDER_DEFAULT = "./data/seed/";

    public static final String JSON_FILE_PARAMS_APP = "params_app.json";
    public static final String JSON_FILE_CUSTOMER = "customers.json";
    public static final String JSON_FILE_WALLET = "wallets.json";
    public static final String JSON_FILE_TRANSACTION = "transactions.json";
    public static final String JSON_FILE_MOVIMENT = "movements.json";
    public static final String JSON_FILE_DEPOSIT_SENDER = "deposit_senders.json";
    public static final String JSON_FILE_LOGIN_AUTH = "login_auth.json";

    // Define o fuso horário do Brasil (ex: America/Sao_Paulo)
    static ZoneId zoneIdBrazil = ZoneId.of("America/Sao_Paulo");

    static int OLD_MINUTES_CREATED = 10;

    /**
     * Escreve uma string de conteúdo em um arquivo no diretório /target/test-data/.
     * 
     * @param fileName O nome do arquivo (ex: "customers.json")
     * @param content  A string (JSON) a ser escrita no arquivo.
     */
    public static void writeStringToFile(String fileName, String content) {
        try {
            // Cria o diretório de destino se não existir (geralmente em /target/test-data/)
            Path filePath = Paths.get(fileName).normalize();

            // Define o caminho completo do arquivo
            //Path parentDir = filePath.resolve(fileName);

            Path parentDir = filePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            // Escreve a string no arquivo usando UTF-8
            Files.writeString(filePath, 
                content,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING 
                );

            System.out.println("Conteúdo salvo com sucesso em: " + parentDir.toAbsolutePath());
            
        } catch (IOException e) {
            System.err.println("Erro ao salvar o arquivo: " + fileName);
            e.printStackTrace();
            
        }
    }

    /**
     * Verifica se um arquivo existe e se foi criado há mais de 2 minutos.
     * 
     * @param fileName O caminho completo do arquivo.
     * @return true se o arquivo existir E sua data de criação for superior a 2
     *         minutos atrás, false caso contrário.
     */
    public static boolean isOlderThanTenMinutes(String fileName) {

        try {
            Path filePath = Paths.get(fileName).normalize();

            if (!Files.exists(filePath)) {
                return true;
            }

            BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
            FileTime lastModifiedTime = attrs.lastModifiedTime();

            Instant lastModifiedInstant = lastModifiedTime.toInstant();
            Instant fiveMinuteAgo = Instant.now().minus(OLD_MINUTES_CREATED, ChronoUnit.MINUTES);

            // --- APENAS MUDANÇAS PARA IMPRESSÃO CORRETA ---
            ZonedDateTime lastModifiedZoned = lastModifiedInstant.atZone(zoneIdBrazil);
            ZonedDateTime tenMinuteAgoZoned = fiveMinuteAgo.atZone(zoneIdBrazil);


            if (lastModifiedZoned.isBefore(tenMinuteAgoZoned)) {
                System.out.println("Arquivo criado em: " + lastModifiedZoned);
                System.out.println(String.format("Corte (%n min atrás): " + tenMinuteAgoZoned, OLD_MINUTES_CREATED));
                System.out.println(String.format("Resultado: O arquivo é MAIS NOVO ou tem %n minutos.", OLD_MINUTES_CREATED));
                return true;
            } else {
                System.out.println("Arquivo criado em: " + lastModifiedZoned);
                System.out.println(String.format("Corte (%n min atrás): " + tenMinuteAgoZoned, OLD_MINUTES_CREATED));
                System.out.println(String.format("Resultado: O arquivo é MAIS NOVO ou tem %n minutos.", OLD_MINUTES_CREATED));
                return false;
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler os atributos do arquivo: " + e.getMessage());
            return false;
        }
    }

    public static <T> List<T> loadJSONToListObject(String filePathString, Class<T> clazz) {

         Path filePath = Paths.get(filePathString).normalize();
        
        if (!Files.exists(filePath)) {
            throw new RuntimeException("Arquivo não encontrado no Sistema de Arquivos: " + filePathString);
        }
        
        try (InputStream stream = Files.newInputStream(filePath)) { // Use Files.newInputStream
            ObjectMapper objectMapper = instanceObjectMapper();
            
            return objectMapper.readValue(
                    stream,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, clazz)
            );
            
        } catch (Exception e) {
            throw new RuntimeException("Erro carregando JSON: " + filePathString, e);
        }
    }

    public static Path getResourcePath() {
        // Obtém o diretório raiz do projeto (ex: C:\Users\...\wallet-service-api)
        String projectRoot = System.getProperty("user.dir");
        
        // Constrói o caminho completo até a pasta resources e o arquivo desejado
        Path fullPath = Paths.get(projectRoot, "src", "main", "resources");
        
        return fullPath;
    }

    public static ObjectMapper instanceObjectMapper() {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // Mantenha false, mas é bom saber
            mapper.findAndRegisterModules(); // Importante para LocalDateTime
            //mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true); // Tente TRUE

            if (!mapper.getRegisteredModuleIds().contains("jackson-datatype-jsr310")) {
                mapper.registerModule(new JavaTimeModule());
            }

        return mapper;
    }

    public static boolean hasFile(String fileref) {
            Path filePath = Paths.get(fileref).normalize();
            return Files.exists(filePath);
    }

}