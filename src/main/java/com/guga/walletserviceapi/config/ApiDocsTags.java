package com.guga.walletserviceapi.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "springdoc")
@Getter @Setter
@NoArgsConstructor
public class ApiDocsTags {
    private List<TagConfig> orderedTags = new ArrayList<>();

    @Getter @Setter @NoArgsConstructor
    public static class TagConfig {
        private String name;
        private String description;
    }
}
