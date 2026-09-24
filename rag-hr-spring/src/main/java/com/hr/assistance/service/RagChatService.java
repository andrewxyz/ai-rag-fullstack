package com.hr.assistance.service;

import com.hr.assistance.config.RagProperties;
import com.hr.assistance.dto.ChatResponse;
import com.hr.assistance.dto.SourceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RagChatService {

    private static final Logger log =
            LoggerFactory.getLogger(RagChatService.class);

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final RagProperties ragProperties;

    public RagChatService(ChatClient.Builder chatClientBuilder,
                          VectorStore vectorStore,
                          RagProperties ragProperties) {

        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
        this.ragProperties = ragProperties;
    }

    public ChatResponse ask(String question) {

        log.info("User Question: {}", question);

        List<Document> relevantDocuments = retrieveRelevantDocuments(question);

        String context = buildContext(relevantDocuments);

        String answer = generateAnswer(question, context);

        List<SourceResponse> sources = buildSources(relevantDocuments);

        return new ChatResponse(answer, sources);
    }

    private List<Document> retrieveRelevantDocuments(String question) {

        // Lowercase so the embedding is consistent regardless of user's capitalisation
        SearchRequest searchRequest = SearchRequest.builder()
                .query(question.toLowerCase())
                .topK(ragProperties.topK())
                .similarityThreshold(ragProperties.similarityThreshold())
                .build();

        List<Document> documents =
                vectorStore.similaritySearch(searchRequest);

        log.info("Retrieved {} documents", documents.size());

        for (int i = 0; i < documents.size(); i++) {

            log.info("-----------------------------");
            log.info("Chunk {}", i + 1);
            log.info("Metadata : {}", documents.get(i).getMetadata());
            log.info("Content : {}", documents.get(i).getText());
        }

        return documents;
    }

    private String buildContext(List<Document> documents) {

        if (documents == null || documents.isEmpty()) {
            return "No relevant context found.";
        }

        String context =  documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        log.info("Generated Context:\n{}", context);

        return context;
    }

    private String generateAnswer(String question, String context) {

        PromptTemplate promptTemplate = new PromptTemplate(
                """
                         Context:
                         {context}
                                        
                         User question:
                         {question}
                """
        );

        String prompt = promptTemplate.render(Map.of(
                "context", context,
                "question",question
        ));

        log.info("Prompt Sent To LLM:\n{}", prompt);

        return chatClient
                .prompt()
                .system("""
                        You are an HR policy assistant.

                        Your job is to answer employee questions using only the provided company policy context.

                        Rules:
                        1. Use only the context provided below.
                        2. Do not use outside knowledge.
                        3. If the context does not contain the answer, say:
                           "I could not find this information in the company policy document."
                        4. Keep the answer short, clear, and helpful.
                        5. Mention the exact limit or condition if it is present in the context.
                        """)
                .user(prompt)
                .call()
                .content();
    }

    private List<SourceResponse> buildSources(List<Document> documents) {

        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        return documents.stream()
                .map(document -> new SourceResponse(
                        String.valueOf(document.getMetadata().getOrDefault("source", "unknown")),
                        String.valueOf(document.getMetadata().getOrDefault("page_number", "unknown")),
                        trim(document.getText())
                ))
                .toList();
    }

    private String trim(String text) {

        if (text == null) {
            return "";
        }

        if (text.length() <= 300) {
            return text;
        }

        return text.substring(0, 300) + "...";
    }
}