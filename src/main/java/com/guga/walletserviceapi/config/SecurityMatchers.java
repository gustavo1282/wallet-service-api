package com.guga.walletserviceapi.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.guga.walletserviceapi.exception.ResourceBadRequestException;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SecurityMatchers {
    private String[] publicPaths;
    private String[] documentation;
    private String[] monitor;
    private String[] secured;
    private String[] admin;

    public List<String> getAllMatchers() {
        List<String> allPublicPatterns = new ArrayList<>();

        if (getPublicPaths() != null) {
            allPublicPatterns.addAll( List.of(publicPaths) );
        }
        if (getDocumentation() != null) {
            allPublicPatterns.addAll( List.of(getDocumentation()) );
        }
        if (getMonitor() != null) {
            allPublicPatterns.addAll( List.of(getMonitor()) );
        }
        if (getSecured() != null) {
            allPublicPatterns.addAll( List.of(getSecured()) );
        }
        if (getAdmin() != null) {
            allPublicPatterns.addAll( List.of(getAdmin() ) );
        }

        if (allPublicPatterns == null || allPublicPatterns.size() == 0){
            throw new ResourceBadRequestException("Nenhum matcher foi encontrado.");
        }

        return allPublicPatterns.stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    @PostConstruct
    public void logMatchers() {
        System.out.println("SecurityMatchers loaded:");
        System.out.println("publicPaths: " + Arrays.toString(publicPaths));
        System.out.println("documentation: " + Arrays.toString(documentation));
        System.out.println("monitor: " + Arrays.toString(monitor));
        System.out.println("secured: " + Arrays.toString(secured));
        System.out.println("admin: " + Arrays.toString(admin));
    }
}