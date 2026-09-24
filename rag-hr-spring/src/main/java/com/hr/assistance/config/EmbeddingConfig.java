package com.hr.assistance.config;

import org.springframework.ai.transformers.TransformersEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.UrlResource;

@Configuration
public class EmbeddingConfig {

    @Value("${spring.ai.embedding.transformer.onnx.modelUri}")
    private String modelUri;

    @Value("${spring.ai.embedding.transformer.tokenizer.uri}")
    private String tokenizerUri;

    @Bean
    @Primary
    public TransformersEmbeddingModel embeddingModel() throws Exception {
        var model = new TransformersEmbeddingModel();
        model.setModelResource(new UrlResource(modelUri));
        model.setTokenizerResource(new UrlResource(tokenizerUri));
        return model;
    }
}
