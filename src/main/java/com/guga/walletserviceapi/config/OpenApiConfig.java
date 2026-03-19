package com.guga.walletserviceapi.config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig implements WebMvcConfigurer {

    @Autowired
    private ApiDocsTags tagProperties;

    @Autowired
    private ApiDocsResponses apiDocsResponses;

    @Value("${app.api-prefix:}")
    private String servletPath;

    @Value("${springdoc.info.title:Wallet Service API}")
    private String apiTitle;

    @Value("${springdoc.info.description:Descrição da API}")
    private String apiDescription;

    @Value("${springdoc.info.version:9.9.9}")
    private String apiVersion;

    private final String packageController = "com.guga.walletserviceapi.controller";

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(servletPath, HandlerTypePredicate.forBasePackage(packageController));
    }

    @Bean
    public OpenAPI walletOpenAPI() {
        Components components = new Components()
            .addSecuritySchemes("bearerAuth",
                new SecurityScheme()
                    .name("bearerAuth")
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
            );

        if (components.getSchemas() == null) components.setSchemas(new HashMap<>());

        // Schema global de erro
        components.addSchemas("ErrorResponse",
            new Schema<>()
                .type("object")
                .addProperty("timestamp", new StringSchema().format("date-time"))
                .addProperty("status", new IntegerSchema().format("int32"))
                .addProperty("code", new StringSchema())
                .addProperty("message", new StringSchema())
                .addProperty("source", new StringSchema())
                .addProperty("path", new StringSchema())
                .addProperty("path", new StringSchema())
                .addProperty("traceId", new StringSchema())
        );

        // Responses globais nomeadas (seu properties ApiDocsResponses)
        if (apiDocsResponses != null && apiDocsResponses.getApiResponses() != null) {
            for (ApiDocsResponses.ResponseConfig cfg : apiDocsResponses.getApiResponses()) {
                components.addResponses(cfg.getKey(),
                    new ApiResponse()
                        .description(cfg.getDescription())
                        .content(new Content().addMediaType("application/json",
                            new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"))
                        ))
                );
            }
        }

        // IMPORTANTE: não setar tags aqui, deixa o springdoc montar.
        // O customizer vai deduplicar e ordenar.
        return new OpenAPI()
            .info(new io.swagger.v3.oas.models.info.Info()
                .title(apiTitle)
                .description(apiDescription)
                .version(apiVersion))
            .components(components);
    }

    /**
     * ✅ Resultado final:
     * - Root tags (openapi.tags): deduplicadas + ordenadas (ordered-tags primeiro, depois alfabético)
     * - Paths: ordenados por (tagOrder, operationId) independentemente do método
     * - Métodos do mesmo path: reatribuídos em ordem de operationId
     */
    @Bean
    public OpenApiCustomizer sortByTagThenOperationId() {
        return openApi -> {
            if (openApi == null) return;

            // 0) mapa de ordem das tags conforme seu ordered-tags
            Map<String, Integer> tagOrder = buildTagOrder();

            // 1) tags root: dedup + order
            normalizeRootTags(openApi, tagOrder);

            // 2) paths e métodos: ordenação
            if (openApi.getPaths() == null || openApi.getPaths().isEmpty()) return;

            // 2.1) reordena métodos de cada path por operationId
            openApi.getPaths().values().forEach(this::sortPathItemMethodsByOperationId);

            // 2.2) reordena os paths por (tag, operationId)
            var entries = new ArrayList<>(openApi.getPaths().entrySet());
            entries.sort(Comparator
                .comparingInt((Map.Entry<String, PathItem> e) -> minTagIndex(e.getValue(), tagOrder))
                .thenComparing(e -> minOperationId(e.getValue()), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER)
            );

            Paths newPaths = new Paths();
            for (var e : entries) newPaths.addPathItem(e.getKey(), e.getValue());
            openApi.setPaths(newPaths);
        };
    }

    // --------------------------
    // Helpers (somente o necessário)
    // --------------------------

    private Map<String, Integer> buildTagOrder() {
        Map<String, Integer> tagOrder = new HashMap<>();
        if (tagProperties != null && tagProperties.getOrderedTags() != null) {
            for (int i = 0; i < tagProperties.getOrderedTags().size(); i++) {
                tagOrder.put(tagProperties.getOrderedTags().get(i).getName(), i);
            }
        }
        return tagOrder;
    }

    /**
     * Deduplica openapi.tags por name e ordena:
     * 1) tags do ordered-tags na ordem
     * 2) demais tags em ordem alfabética
     *
     * Mantém a melhor description disponível:
     * - se a tag configurada não tem description, tenta aproveitar a do springdoc
     */
    private void normalizeRootTags(OpenAPI openApi, Map<String, Integer> tagOrder) {
        Map<String, io.swagger.v3.oas.models.tags.Tag> unique = new LinkedHashMap<>();

        // A) primeiro: ordered-tags
        if (tagProperties != null && tagProperties.getOrderedTags() != null) {
            for (var cfg : tagProperties.getOrderedTags()) {
                var t = new io.swagger.v3.oas.models.tags.Tag();
                t.setName(cfg.getName());
                if (cfg.getDescription() != null && !cfg.getDescription().isBlank()) {
                    t.setDescription(cfg.getDescription());
                }
                unique.put(cfg.getName(), t);
            }
        }

        // B) merge: tags já existentes (springdoc)
        if (openApi.getTags() != null) {
            for (var existing : openApi.getTags()) {
                if (existing == null || existing.getName() == null) continue;
                String name = existing.getName();

                var current = unique.get(name);
                if (current == null) {
                    unique.put(name, existing); // não configurada -> adiciona
                } else {
                    // configurada -> só completa description se estiver faltando
                    if ((current.getDescription() == null || current.getDescription().isBlank())
                        && existing.getDescription() != null && !existing.getDescription().isBlank()) {
                        current.setDescription(existing.getDescription());
                    }
                }
            }
        }

        // C) ordena as não-configuradas em alfabético (as configuradas já estão no topo)
        int configuredCount = (tagProperties != null && tagProperties.getOrderedTags() != null)
            ? tagProperties.getOrderedTags().size()
            : 0;

        List<io.swagger.v3.oas.models.tags.Tag> finalTags = new ArrayList<>(unique.values());

        if (configuredCount > 0 && finalTags.size() > configuredCount) {
            List<io.swagger.v3.oas.models.tags.Tag> head =
                new ArrayList<>(finalTags.subList(0, configuredCount));
            List<io.swagger.v3.oas.models.tags.Tag> tail =
                new ArrayList<>(finalTags.subList(configuredCount, finalTags.size()));

            tail.sort(Comparator.comparing(io.swagger.v3.oas.models.tags.Tag::getName, String.CASE_INSENSITIVE_ORDER));

            List<io.swagger.v3.oas.models.tags.Tag> merged = new ArrayList<>();
            merged.addAll(head);
            merged.addAll(tail);
            openApi.setTags(merged);
        } else {
            // sem ordered-tags -> só dedup e alfabético
            if (configuredCount == 0) {
                finalTags.sort(Comparator.comparing(io.swagger.v3.oas.models.tags.Tag::getName, String.CASE_INSENSITIVE_ORDER));
            }
            openApi.setTags(finalTags);
        }
    }

    private int minTagIndex(PathItem pathItem, Map<String, Integer> tagOrder) {
        return pathItem.readOperations().stream()
            .flatMap(op -> op.getTags() == null ? java.util.stream.Stream.empty() : op.getTags().stream())
            .map(t -> tagOrder.getOrDefault(t, Integer.MAX_VALUE))
            .min(Integer::compareTo)
            .orElse(Integer.MAX_VALUE);
    }

    private String minOperationId(PathItem pathItem) {
        return pathItem.readOperations().stream()
            .map(Operation::getOperationId)
            .filter(id -> id != null && !id.isBlank())
            .min(String.CASE_INSENSITIVE_ORDER)
            .orElse("ZZZ_NO_ID");
    }

    private void sortPathItemMethodsByOperationId(PathItem pathItem) {
        var opsMap = pathItem.readOperationsMap();
        if (opsMap == null || opsMap.isEmpty()) return;

        var list = new ArrayList<>(opsMap.entrySet());
        list.sort(Comparator.comparing(e -> safeOpId(e.getValue()), String.CASE_INSENSITIVE_ORDER));

        // limpa todos os methods
        pathItem.setGet(null);
        pathItem.setPost(null);
        pathItem.setPut(null);
        pathItem.setDelete(null);
        pathItem.setPatch(null);
        pathItem.setHead(null);
        pathItem.setOptions(null);
        pathItem.setTrace(null);

        // reatribui em ordem
        for (var e : list) {
            switch (e.getKey()) {
                case GET -> pathItem.setGet(e.getValue());
                case POST -> pathItem.setPost(e.getValue());
                case PUT -> pathItem.setPut(e.getValue());
                case DELETE -> pathItem.setDelete(e.getValue());
                case PATCH -> pathItem.setPatch(e.getValue());
                case HEAD -> pathItem.setHead(e.getValue());
                case OPTIONS -> pathItem.setOptions(e.getValue());
                case TRACE -> pathItem.setTrace(e.getValue());
            }
        }
    }

    private String safeOpId(Operation op) {
        if (op == null) return "ZZZ_NO_ID";
        String id = op.getOperationId();
        return (id == null || id.isBlank()) ? "ZZZ_NO_ID" : id;
    }
}