package com.hr.assistance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.rag")
public record RagProperties(
        String documentPath,
        int topK,
        @DefaultValue("0.30") double similarityThreshold,
        @DefaultValue("uploads") String uploadDir
) {
}
