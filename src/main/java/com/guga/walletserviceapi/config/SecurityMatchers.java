package com.guga.walletserviceapi.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;

import com.guga.walletserviceapi.exception.ResourceBadRequestException;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SecurityMatchers {
    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Value("${app.api-prefix:}")
    private String servletPath;

    private String[] publicPaths;
    private String[] documentation;
    private String[] monitor;
    private String[] secured;
    private String[] admin;
    private String[] permitAllPaths;

    private String[] addContextPath(String[] paths) {
        return paths;
    }

    public String[] getPublicPaths() {
        return addContextPath(publicPaths);
    }

    public String[] getDocumentation() {
        return addContextPath(documentation);
    }

    public String[] getMonitor() {
        return addContextPath(monitor);
    }

    public String[] getSecured() {
        return addContextPath(secured);
    }

    public String[] getAdmin() {
        return addContextPath(admin);
    }
    
    public String[] getPermitAllPaths() {
        return addContextPath(permitAllPaths);
    }


    public List<String> getAllMatchers() {
        List<String> allPublicPatterns = new ArrayList<>();

        if (getPublicPaths() != null) {
            allPublicPatterns.addAll( List.of(getPublicPaths()) );
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
        if (getPermitAllPaths() != null) {
            allPublicPatterns.addAll( List.of(getPermitAllPaths()) );
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
        System.out.println("SecurityMatchers loaded with context-path: " + contextPath);
        System.out.println("publicPaths: " + Arrays.toString(getPublicPaths()));
        System.out.println("documentation: " + Arrays.toString(getDocumentation()));
        System.out.println("monitor: " + Arrays.toString(getMonitor()));
        System.out.println("secured: " + Arrays.toString(getSecured()));
        System.out.println("admin: " + Arrays.toString(getAdmin()));
        System.out.println("permitpaths: " + Arrays.toString(getPermitAllPaths()));
    }
}