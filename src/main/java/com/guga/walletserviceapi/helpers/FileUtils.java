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

    public static final String FOLDER_DEFAULT_FILE_JSON = "./data/seed/";

    public static final String JSON_FILE_CUSTOMER = "customers.json";
    public static final String JSON_FILE_WALLET = "wallets.json";
    public static final String JSON_FILE_TRANSACTION = "transactions.json";
    public static final String JSON_FILE_MOVIMENT = "movements.json";
    public static final String JSON_FILE_DEPOSIT_SENDER = "deposit_senders.json";

    // Define o fuso horário do Brasil (ex: America/Sao_Paulo)
    static ZoneId zoneIdBrazil = ZoneId.of("America/Sao_Paulo");

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
    public static boolean isOlderThanFiveMinutes(String fileName) {

        try {
            Path filePath = Paths.get(fileName).normalize();

            if (!Files.exists(filePath)) {
                return true;
            }

            BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
            FileTime lastModifiedTime = attrs.lastModifiedTime();

            Instant lastModifiedInstant = lastModifiedTime.toInstant();
            Instant fiveMinuteAgo = Instant.now().minus(5, ChronoUnit.MINUTES);

            // --- APENAS MUDANÇAS PARA IMPRESSÃO CORRETA ---
            ZonedDateTime lastModifiedZoned = lastModifiedInstant.atZone(zoneIdBrazil);
            ZonedDateTime fiveMinuteAgoZoned = fiveMinuteAgo.atZone(zoneIdBrazil);


            if (lastModifiedZoned.isBefore(fiveMinuteAgoZoned)) {
                System.out.println("Arquivo criado em: " + lastModifiedZoned);
                System.out.println("Corte (5 min atrás): " + fiveMinuteAgoZoned);
                System.out.println("Resultado: O arquivo é MAIS ANTIGO que 5 minutos.");
                return true;
            } else {
                System.out.println("Arquivo criado em: " + lastModifiedZoned);
                System.out.println("Corte (5 min atrás): " + fiveMinuteAgoZoned);
                System.out.println("Resultado: O arquivo é MAIS NOVO ou tem 5 minutos.");
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
        /*try {
            
            ClassPathResource resource = new ClassPathResource(path);
            
            if (!resource.exists()) {
                throw new RuntimeException("Arquivo não encontrado no Classpath: " + path);
            }
            
            InputStream stream = resource.getInputStream();
            
            return objectMapper.readValue(
                    stream,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, clazz)
            );
        } catch (Exception e) {
            System.err.println("DEBUG INFO: Erro real ao carregar JSON:");
            e.printStackTrace(); // Imprime o erro original no console
            throw new RuntimeException("Erro carregando JSON: " + path, e);
        }*/
    }

    // Assumindo que você tem uma classe Transaction
/*    public static <T> List<T> loadListFromFile(String fileName,
            TypeReference<List<T>> typeRef) {
        ObjectMapper mapper = GlobalHelper.instanceObjectMapper();

        try {
            Path directoryPath = Paths.get(FileUtils.FOLDER_DEFAULT_FILE_JSON);
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
*/
    /*
    public static void loadJSONToStrinClass(MultipartFile file) {
        //InputStream file = InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
        
        ObjectMapper mapper = new ObjectMapper();

        // Garante que o ObjectMapper lide com LocalDate e LocalDateTime antes de
        // carregar os JSONs
        if (!mapper.getRegisteredModuleIds().contains("jackson-datatype-jsr310")) {
            mapper.registerModule(new JavaTimeModule());
        }


        
        try {
 

            return mapper.readValue(file, typeRef);
        } catch (IOException e) {
            System.err.println("Erro ao carregar o arquivo: " + fileName);
            e.printStackTrace();
            return null;
        }
    }
    */            


    /*
    public static String convertCsvToJson(Reader reader) throws IOException, CsvValidationException {
        // 1. Constrói o CSVParser, que define as regras do seu CSV (vírgula, aspas, etc.)
        // O construtor CSVParserBuilder() deve funcionar
        var parser = new CSVParserBuilder()
            .withSeparator(',') 
            .withIgnoreLeadingWhiteSpace(true)
            .build();

        // 2. Constrói o CSVReader, que aceita o Reader e o Parser
        // O construtor CSVReaderBuilder(Reader) e .withCSVParser(parser).build() deve funcionar
        try (CSVReader csvReader = new CSVReaderBuilder(reader)
            .withCSVParser(parser)
            .build()) {

            // 3. Lê a primeira linha para obter o CABEÇALHO
            String[] header = csvReader.readNext();

            if (header == null || header.length == 0) {
                return "[]"; // Retorna JSON vazio se o arquivo estiver vazio
            }

            // 4. Inicializa a lista de registros
            List<Map<String, String>> records = new ArrayList<>();
            String[] line;
            
            // 5. Itera sobre as linhas de dados restantes
            while ((line = csvReader.readNext()) != null) {
                
                // Usamos LinkedHashMap para manter a ordem das chaves (opcional, mas bom para debug)
                Map<String, String> recordMap = new LinkedHashMap<>();
                
                // 6. Itera sobre o cabeçalho e os dados, mapeando coluna a coluna
                for (int i = 0; i < header.length; i++) {
                    String key = header[i];
                    // Evita ArrayIndexOutOfBounds se a linha tiver menos colunas que o cabeçalho
                    String value = (i < line.length) ? line[i] : ""; 
                    
                    recordMap.put(key, value);
                }
                
                records.add(recordMap);
            }
            
            // 7. Usa o Jackson para serializar a lista de Mapas para JSON
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(records);
        }
    }
     */
    
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

}