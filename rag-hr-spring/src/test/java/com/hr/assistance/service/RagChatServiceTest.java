package com.hr.assistance.service;

import com.hr.assistance.config.RagProperties;
import com.hr.assistance.dto.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagChatServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private VectorStore vectorStore;

    private RagChatService service;

    @BeforeEach
    void setUp() {
        ChatClient chatClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()
                .system(anyString())
                .user(anyString())
                .call()
                .content())
            .thenReturn("Mock AI answer");

        var ragProperties = new RagProperties("documents/company-leave-policy.pdf", 4, 0.10);
        service = new RagChatService(chatClientBuilder, vectorStore, ragProperties);
    }

    @Test
    void ask_whenDocumentsFound_shouldReturnAnswerWithSources() {
        var doc = new Document("Annual leave content", Map.of(
                "source", "company-leave-policy.pdf",
                "page_number", "1"
        ));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc));

        ChatResponse response = service.ask("How many annual leave days do I get?");

        assertThat(response.answer()).isEqualTo("Mock AI answer");
        assertThat(response.sources()).hasSize(1);
        assertThat(response.sources().get(0).source()).isEqualTo("company-leave-policy.pdf");
        assertThat(response.sources().get(0).page()).isEqualTo("1");
        assertThat(response.sources().get(0).content()).isEqualTo("Annual leave content");
    }

    @Test
    void ask_whenNoDocumentsFound_shouldReturnAnswerWithEmptySources() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        ChatResponse response = service.ask("What is the expense reimbursement policy?");

        assertThat(response.answer()).isEqualTo("Mock AI answer");
        assertThat(response.sources()).isEmpty();
    }

    @Test
    void ask_whenDocumentTextExceeds300Chars_shouldTrimSourceContentAndAppendEllipsis() {
        String longText = "A".repeat(400);
        var doc = new Document(longText, Map.of("source", "policy.pdf", "page_number", "2"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc));

        ChatResponse response = service.ask("What is the leave policy?");

        String content = response.sources().get(0).content();
        assertThat(content).hasSize(303); // 300 chars + "..."
        assertThat(content).endsWith("...");
    }

    @Test
    void ask_whenDocumentTextIsExactly300Chars_shouldNotTrim() {
        String exactText = "B".repeat(300);
        var doc = new Document(exactText, Map.of("source", "policy.pdf", "page_number", "1"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc));

        ChatResponse response = service.ask("question?");

        assertThat(response.sources().get(0).content()).isEqualTo(exactText);
        assertThat(response.sources().get(0).content()).doesNotEndWith("...");
    }

    @Test
    void ask_whenDocumentHasNoSourceMetadata_shouldFallbackToUnknown() {
        var doc = new Document("Some policy content", Map.of());
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc));

        ChatResponse response = service.ask("question?");

        assertThat(response.sources().get(0).source()).isEqualTo("unknown");
        assertThat(response.sources().get(0).page()).isEqualTo("unknown");
    }

    @Test
    void ask_whenMultipleDocumentsFound_shouldReturnAllSources() {
        var doc1 = new Document("Content 1", Map.of("source", "policy.pdf", "page_number", "1"));
        var doc2 = new Document("Content 2", Map.of("source", "policy.pdf", "page_number", "2"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc1, doc2));

        ChatResponse response = service.ask("sick leave?");

        assertThat(response.sources()).hasSize(2);
    }

    @Test
    void ask_shouldQueryVectorStoreWithSearchRequest() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        service.ask("maternity leave?");

        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }
}
