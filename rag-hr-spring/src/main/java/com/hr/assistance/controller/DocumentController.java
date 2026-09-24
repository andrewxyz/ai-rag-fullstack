package com.hr.assistance.controller;

import com.hr.assistance.service.DocumentIngestionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
public class DocumentController {

    private final DocumentIngestionService documentIngestionService;

    public DocumentController(DocumentIngestionService documentIngestionService) {
        this.documentIngestionService = documentIngestionService;
    }

    @PostMapping("/api/documents/ingest")
    public String ingestDocument() {
        return documentIngestionService.ingestLeavePolicy();
    }

    @PostMapping(value = "/api/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadDocument(@RequestParam("file") MultipartFile file) throws IOException {
        return documentIngestionService.ingestUploadedFile(file);
    }
}
