package com.hr.assistance.service;

import com.hr.assistance.config.RagProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

    private static final String SOURCE_FILE_NAME = "company-leave-policy.pdf";
    private static final String DOCUMENT_TYPE = "HR_LEAVE_POLICY";

    private final VectorStore vectorStore;
    private final RagProperties ragProperties;
    private final JdbcTemplate jdbcTemplate;

    public DocumentIngestionService(VectorStore vectorStore, RagProperties ragProperties, JdbcTemplate jdbcTemplate) {
        this.vectorStore = vectorStore;
        this.ragProperties = ragProperties;
        this.jdbcTemplate = jdbcTemplate;
    }

    public String ingestLeavePolicy() {
        clearVectorStore();

        ClassPathResource resource = new ClassPathResource(ragProperties.documentPath());
        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource);

        List<Document> pages = pdfReader.get();
        log.info("No. of document pages " + pages.size());

        String documentId = UUID.randomUUID().toString();
        String ingestedAt = Instant.now().toString();

        List<Document> enrichedPages = pages.stream()
                .map(document -> addMetadata(document, SOURCE_FILE_NAME, documentId, ingestedAt))
                .toList();

        TokenTextSplitter textSplitter = new TokenTextSplitter();
        List<Document> chunks = textSplitter.apply(enrichedPages);

        vectorStore.add(chunks);

        return "Successfully ingested " + chunks.size() + " chunks from " + SOURCE_FILE_NAME;
    }

    public String ingestUploadedFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new IllegalArgumentException("Only PDF files are supported");
        }

        clearVectorStore();

        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.pdf";
        String savedFilename = timestamp + "_" + originalFilename;

        Path uploadDir = Paths.get(ragProperties.uploadDir()).toAbsolutePath();
        Files.createDirectories(uploadDir);
        Path savedPath = uploadDir.resolve(savedFilename);
        try (var inputStream = file.getInputStream()) {
            Files.copy(inputStream, savedPath, StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("Saved uploaded file to {}", savedPath);

        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(new FileSystemResource(savedPath));
        List<Document> pages = pdfReader.get();
        log.info("No. of document pages {}", pages.size());

        String documentId = UUID.randomUUID().toString();
        String ingestedAt = Instant.now().toString();

        List<Document> enrichedPages = pages.stream()
                .map(document -> addMetadata(document, originalFilename, documentId, ingestedAt))
                .toList();

        TokenTextSplitter textSplitter = new TokenTextSplitter();
        List<Document> chunks = textSplitter.apply(enrichedPages);

        vectorStore.add(chunks);

        return "Successfully ingested " + chunks.size() + " chunks from " + originalFilename;
    }

    private void clearVectorStore() {
        int deleted = jdbcTemplate.update("DELETE FROM vector_store");
        log.info("Cleared {} chunks from vector store before ingestion", deleted);
    }

    private Document addMetadata(Document document, String sourceName, String documentId, String ingestedAt) {
        Map<String, Object> metadata = new HashMap<>(document.getMetadata());
        metadata.put("documentId", documentId);
        metadata.put("source", sourceName);
        metadata.put("documentType", DOCUMENT_TYPE);
        metadata.put("ingestedAt", ingestedAt);
        return new Document(document.getText(), metadata);
    }
}
