package com.hr.assistance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hr.assistance.config.EmbeddingConfig;
import com.hr.assistance.dto.ChatRequest;
import com.hr.assistance.dto.ChatResponse;
import com.hr.assistance.dto.SourceResponse;
import com.hr.assistance.service.RagChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = ChatController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = EmbeddingConfig.class
        )
)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RagChatService ragChatService;

    @Test
    void chat_withValidQuestion_shouldReturn200WithAnswer() throws Exception {
        var sources = List.of(new SourceResponse("company-leave-policy.pdf", "1", "Annual leave content"));
        when(ragChatService.ask(anyString()))
                .thenReturn(new ChatResponse("You get 24 annual leave days.", sources));

        mockMvc.perform(post("/api/v1/rag/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChatRequest("How many leave days?"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("You get 24 annual leave days."))
                .andExpect(jsonPath("$.sources[0].source").value("company-leave-policy.pdf"))
                .andExpect(jsonPath("$.sources[0].page").value("1"));
    }

    @Test
    void chat_withBlankQuestion_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/rag/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chat_withMissingQuestionField_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/rag/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chat_withNoDocumentsFound_shouldReturnAnswerWithEmptySources() throws Exception {
        when(ragChatService.ask(anyString()))
                .thenReturn(new ChatResponse("I could not find this information.", List.of()));

        mockMvc.perform(post("/api/v1/rag/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\": \"What is the expense policy?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("I could not find this information."))
                .andExpect(jsonPath("$.sources").isEmpty());
    }
}
