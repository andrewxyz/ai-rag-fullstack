package com.hr.assistance.controller;

import com.hr.assistance.config.EmbeddingConfig;
import com.hr.assistance.service.DocumentIngestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = DocumentController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = EmbeddingConfig.class
        )
)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentIngestionService documentIngestionService;

    @Test
    void ingestDocument_shouldReturn200WithSuccessMessage() throws Exception {
        when(documentIngestionService.ingestLeavePolicy())
                .thenReturn("Successfully ingested 5 chunks from company-leave-policy.pdf");

        mockMvc.perform(post("/api/documents/ingest"))
                .andExpect(status().isOk())
                .andExpect(content().string("Successfully ingested 5 chunks from company-leave-policy.pdf"));
    }

    @Test
    void ingestDocument_withDifferentMessage_shouldReturnThatMessage() throws Exception {
        when(documentIngestionService.ingestLeavePolicy())
                .thenReturn("Successfully ingested 12 chunks from company-leave-policy.pdf");

        mockMvc.perform(post("/api/documents/ingest"))
                .andExpect(status().isOk())
                .andExpect(content().string("Successfully ingested 12 chunks from company-leave-policy.pdf"));
    }
}
