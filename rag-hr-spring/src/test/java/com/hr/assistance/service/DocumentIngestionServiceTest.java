package com.hr.assistance.service;

import com.hr.assistance.config.RagProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DocumentIngestionServiceTest {

    @Mock
    private VectorStore vectorStore;

    private DocumentIngestionService service;

    @BeforeEach
    void setUp() {
        var ragProperties = new RagProperties("documents/company-leave-policy.pdf", 4, 0.10);
        service = new DocumentIngestionService(vectorStore, ragProperties);
    }

    @Test
    void ingestLeavePolicy_shouldReturnSuccessMessageWithChunkCount() {
        String result = service.ingestLeavePolicy();

        assertThat(result).startsWith("Successfully ingested");
        assertThat(result).endsWith("chunks from company-leave-policy.pdf");
    }

    @Test
    void ingestLeavePolicy_shouldAddChunksToVectorStore() {
        service.ingestLeavePolicy();

        verify(vectorStore).add(anyList());
    }

    @Test
    void ingestLeavePolicy_chunksAddedToVectorStore_shouldHaveSourceMetadata() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);

        service.ingestLeavePolicy();

        verify(vectorStore).add(captor.capture());
        List<Document> chunks = captor.getValue();

        assertThat(chunks).isNotEmpty();
        chunks.forEach(chunk -> {
            assertThat(chunk.getMetadata()).containsKey("source");
            assertThat(chunk.getMetadata().get("source")).isEqualTo("company-leave-policy.pdf");
        });
    }

    @Test
    void ingestLeavePolicy_chunksAddedToVectorStore_shouldHaveDocumentTypeMetadata() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);

        service.ingestLeavePolicy();

        verify(vectorStore).add(captor.capture());
        List<Document> chunks = captor.getValue();

        chunks.forEach(chunk -> {
            assertThat(chunk.getMetadata()).containsKey("documentType");
            assertThat(chunk.getMetadata().get("documentType")).isEqualTo("HR_LEAVE_POLICY");
        });
    }

    @Test
    void ingestLeavePolicy_chunksAddedToVectorStore_shouldHaveDocumentIdAndIngestedAt() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);

        service.ingestLeavePolicy();

        verify(vectorStore).add(captor.capture());
        List<Document> chunks = captor.getValue();

        chunks.forEach(chunk -> {
            assertThat(chunk.getMetadata()).containsKey("documentId");
            assertThat(chunk.getMetadata()).containsKey("ingestedAt");
            assertThat(chunk.getMetadata().get("documentId")).isNotNull().isInstanceOf(String.class);
            assertThat(chunk.getMetadata().get("ingestedAt")).isNotNull().isInstanceOf(String.class);
        });
    }

    @Test
    void ingestLeavePolicy_allChunks_shouldShareSameDocumentId() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);

        service.ingestLeavePolicy();

        verify(vectorStore).add(captor.capture());
        List<Document> chunks = captor.getValue();

        assertThat(chunks).isNotEmpty();
        String firstDocId = (String) chunks.get(0).getMetadata().get("documentId");
        chunks.forEach(chunk ->
            assertThat(chunk.getMetadata().get("documentId")).isEqualTo(firstDocId)
        );
    }
}
