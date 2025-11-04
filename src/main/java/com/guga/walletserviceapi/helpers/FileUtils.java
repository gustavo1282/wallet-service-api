package com.guga.walletserviceapi.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class FileUtils {

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
            File directory = new File("target/test-data");
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
            Path directoryPath = Paths.get("target/test-data");
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


}