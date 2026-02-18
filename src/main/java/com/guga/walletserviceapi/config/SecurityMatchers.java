package com.guga.walletserviceapi.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import com.guga.walletserviceapi.exception.ResourceBadRequestException;
import com.guga.walletserviceapi.logging.LogMarkers;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SecurityMatchers {

    private static final Logger LOGGER = LogManager.getLogger(SecurityMatchers.class);

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
        //if (paths == null) return null;
        //if (contextPath == null || contextPath.isBlank()) return paths;
        //return Arrays.stream(paths)
        //    .map(path -> path != null && path.startsWith(contextPath) ? path : contextPath + path)
        //    .toArray(String[]::new);
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

        if (allPublicPatterns.size() == 0){
            throw new ResourceBadRequestException("Nenhum matcher foi encontrado.");
        }

        return allPublicPatterns.stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    @PostConstruct
    public void logMatchers() {
       LOGGER.info(LogMarkers.LOG, "SecurityMatchers loaded with context-path: " + contextPath);
       LOGGER.info(LogMarkers.LOG, "publicPaths: " + Arrays.toString(getPublicPaths()));
       LOGGER.info(LogMarkers.LOG, "documentation: " + Arrays.toString(getDocumentation()));
       LOGGER.info(LogMarkers.LOG, "monitor: " + Arrays.toString(getMonitor()));
       LOGGER.info(LogMarkers.LOG, "secured: " + Arrays.toString(getSecured()));
       LOGGER.info(LogMarkers.LOG, "admin: " + Arrays.toString(getAdmin()));
       LOGGER.info(LogMarkers.LOG, "permitpaths: " + Arrays.toString(getPermitAllPaths()));
    }
}
